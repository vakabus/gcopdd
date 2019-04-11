package cz.cuni.mff.d3s.blood.dependencyMatrix;

import cz.cuni.mff.d3s.blood.utils.CheckedConsumer;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.graalvm.compiler.graph.Node;

public final class DependencyMatrixCollector {

    private static DependencyMatrixCollector instance = null;

    // the default of 16 doesn't fit even the most trivial programs
    private static final int HASHMAP_INIT_CAPACITY = 64;

    /**
     * The maximum number of threads that can write to the matrix at once. This
     * number should be large enough to allow potentially all the JVM CI threads
     * to write, but small enough so that calling {@link Semaphore#acquire()}
     * `MAX_WRITERS`-times is still effective.
     */
    private static final int MAX_WRITERS = 128;

    /**
     * The number of threads that are currently writing to the dependency
     * matrix. This is not used to serialize accesses to the matrix, but to
     * disable writing to it when it is being dumped. This is achieved by
     * acquiring all the permits in the dump method.
     */
    private final Semaphore writers = new Semaphore(Integer.MAX_VALUE, true);

    private final ConcurrentHashMap<Class, ConcurrentHashMap<Class, DependencyValue>> dependencyTable = new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY);

    // TODO make this configurable, also works with `new DefaultNodeTracker()`
    private final NodeTracker nodeTracker = new CustomNodeTracker();

    /**
     * getValue.apply(rowKey).apply(columnKey) returns DependencyValue. Returns
     * value from matrix. If it encounters a null on the way, returns
     * {@link DependencyValue#ZERO}.
     */
    private final Function<Class, Function<Class, DependencyValue>> getValue = class1 -> {
        final var row = dependencyTable.get(class1);
        return (row == null)
                ? class2 -> DependencyValue.ZERO
                : class2 -> row.getOrDefault(class2, DependencyValue.ZERO);
    };

    public static DependencyMatrixCollector getInstance() {
        if (instance == null) {
            instance = new DependencyMatrixCollector();
        }
        return instance;
    }

    /**
     * This function is called by the instrumentation after the Node class is
     * initialized.
     */
    public final void onNodeClassInit() {
        // register shutdown hook for dumping data
        Runtime.getRuntime().addShutdownHook(new Thread(this::dump, "dump at exit"));

        // initialize everything needed by the node tracker implementation
        nodeTracker.onNodeClassInit();
    }

    /**
     * This function is called by the instrumentation before every optimization
     * phase run. More specifically, before calling
     * {@link org.graalvm.compiler.phases.BasePhase#apply(StructuredGraph, Object)}
     *
     * @param graph Graph entering the optimization phase
     * @param sourceClass Class of the optimization phase running
     */
    public final void prePhase(StructuredGraph graph, Class<?> sourceClass) {
        if (!writers.tryAcquire()) {
            // Dump is already in progress, don't change the matrix any more.
            return;
        }

        // obtain row in result matrix for this particular optimization phase
        var row = dependencyTable.get(sourceClass);
        if (row == null) {
            row = new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY);
            dependencyTable.putIfAbsent(sourceClass, row);
            row = dependencyTable.get(sourceClass);
        }

        // for each node in the graph entering the phase, note down where it was created
        for (Node node : graph.getNodes()) {
            var creationPhaseResult = nodeTracker.getCreationPhase(node);

            if (creationPhaseResult.isError()) {
                // FIXME do something more meaningful with missing source
                // this happens only when the graph is first loaded
                System.err.println(creationPhaseResult.unwrapError());
                continue;
            }

            var creationPhase = creationPhaseResult.unwrap();

            DependencyValue value = row.get(creationPhase);
            if (value == null) {
                value = new DependencyValue();
                row.putIfAbsent(creationPhase, value);
                value = row.get(creationPhase);
            }

            value.update(1, 0);
        }

        // update total node counts for all tracked values
        for (var value : row.values()) {
            value.update(0, graph.getNodeCount());
        }

        writers.release();
    }

    /**
     * This function is called by the instrumentation after every optimization
     * phase run. More specifically, after calling
     * {@link org.graalvm.compiler.phases.BasePhase#apply(StructuredGraph, Object)}
     *
     * @param graph Graph representing IL after being processed by the
     * optimization phase
     * @param sourceClass Class of the running optimization phase
     */
    public final void postPhase(StructuredGraph graph, Class<?> sourceClass) {
        nodeTracker.updateCreationPhase(graph.getNodes(), sourceClass);
    }

    /**
     * Method called on JVM exit dumping collected statistics.
     */
    private void dump() {
        // Hog all the permits so that nobody can run if dump runs.
        for (int i = 0; i < MAX_WRITERS; i++) {
            writers.acquireUninterruptibly();
        }

        Instant started = Instant.now();

        // collect all phase classes
        final Class[] keysOrder = dependencyTable.entrySet().stream().flatMap(entry
                -> Stream.concat(Stream.of(entry.getKey()), entry.getValue().keySet().stream())
        ).collect(Collectors.toSet()).toArray(i -> new Class[i]);

        try (final OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream("/tmp/gcopdd-depmat"))) {
            // List of classes in the order that is used in the matrix below.
            // Each line contains space-separated list of the class's
            // superclasses from itself up to java.lang.Object.
            Arrays.stream(keysOrder)
                    .map(clazz
                            -> Stream.iterate(clazz, Predicate.isEqual(null).negate(), Class::getSuperclass)
                            .map(Class::getName)
                            .collect(Collectors.joining(" ", "", "\n"))
                    )
                    .forEachOrdered((CheckedConsumer<String>) out::append);

            // Empty line.
            out.append('\n');

            // Dependency matrix. Lines correspond to rows.
            // Items in a row are separated by spaces.
            Arrays.stream(keysOrder)
                    .map(getValue)
                    .map(getValueFromCurrentRow
                            -> Arrays.stream(keysOrder)
                            .map(getValueFromCurrentRow)
                            .map(DependencyValue::toString)
                            .collect(Collectors.joining(" ", "", "\n"))
                    )
                    .forEachOrdered((CheckedConsumer<String>) out::append);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        Instant finished = Instant.now();
        Duration duration = Duration.between(started, finished);
        System.getLogger(DependencyMatrixCollector.class.getName()).log(
                System.Logger.Level.INFO,
                "Dependency matrix dump finished in {0} ms",
                duration.toMillis()
        );
    }
}
