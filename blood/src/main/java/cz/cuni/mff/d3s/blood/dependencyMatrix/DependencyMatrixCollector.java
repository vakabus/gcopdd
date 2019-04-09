package cz.cuni.mff.d3s.blood.dependencyMatrix;

import cz.cuni.mff.d3s.blood.utils.CheckedConsumer;
import cz.cuni.mff.d3s.blood.utils.Result;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DependencyMatrixCollector {

    // the default of 16 doesn't fit even the most trivial programs
    private static final int HASHMAP_INIT_CAPACITY = 64;

    private static final AtomicBoolean collect = new AtomicBoolean(true);
    private static final ConcurrentHashMap<Class, ConcurrentHashMap<Class, DependencyValue>> dependencyTable = new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY);

    /**
     * getValue.apply(rowKey).apply(columnKey) returns DependencyValue. Returns
     * value from matrix. If it encounters a null on the way, returns
     * {@link DependencyValue#ZERO}.
     */
    private static final Function<Class, Function<Class, DependencyValue>> getValue = class1 -> {
        final var row = dependencyTable.get(class1);
        return (row == null)
                ? class2 -> DependencyValue.ZERO
                : class2 -> row.getOrDefault(class2, DependencyValue.ZERO);
    };

    /**
     * This function is called by the instrumentation during compiler initialization
     */
    public static void init() {
        // register shutdown hook for dumping data
        Runtime.getRuntime().addShutdownHook(new Thread(DependencyMatrixCollector::dump, "dump at exit"));
    }

    /**
     * This function is called by the instrumentation before every optimization phase run. More specifically,
     * before calling {@link org.graalvm.compiler.phases.BasePhase#apply(StructuredGraph, Object)}
     *
     * @param graph Graph entering the optimization phase
     * @param sourceClass Class of the optimization phase running
     */
    public static void prePhase(StructuredGraph graph, Class<?> sourceClass) {
        // don't do anything, when not supposed to
        if (!collect.get()) return;

        // obtain row in result matrix for this particular optimization phase
        var row = dependencyTable.get(sourceClass);
        if (row == null) {
            row = new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY);
            dependencyTable.putIfAbsent(sourceClass, row);
        }

        // for each node in the graph entering the phase, note down where it was created
        for (var node : graph.getNodes()) {
            var creationPhase = getCreationPhase(node);
            if (creationPhase.isError()) {
                // FIXME do something more meaningful with missing source
                // this happens only when the graph is first loaded
                System.err.println(creationPhase.unwrapError());
                continue;
            }

            DependencyValue value = row.get(creationPhase.unwrap());
            if (value == null) {
                value = new DependencyValue();
                row.putIfAbsent(creationPhase.unwrap(), value);
            }

            value.update(1, 0);
        }

        // update total node counts for all tracked values
        for (var value : row.values()) {
            value.update(0, graph.getNodeCount());
        }
    }

    /**
     * This function is called by the instrumentation after every optimization phase run. More specifically,
     * after calling {@link org.graalvm.compiler.phases.BasePhase#apply(StructuredGraph, Object)}
     *
     * @param graph Graph representing IL after being processed by the optimization phase
     * @param sourceClass Class of the running optimization phase
     */
    public static void postPhase(StructuredGraph graph, Class<?> sourceClass) {
        // don't do anything, when not supposed to
        if (!collect.get()) return;

        // mark all nodes without any creation annotation as created in this phase
        for (Node node : graph.getNodes()) {
            if (getCreationPhase(node).isError()) {
                setCreationPhase(node, sourceClass);
            }
        }
    }

    /**
     * Method called on JVM exit dumping collected statistics.
     */
    private static void dump() {
        Instant started = Instant.now();

        // disable data collection, because compiler runs even during shutdown
        collect.set(false);

        // collect all phase classes
        final Class[] keysOrder = (Class[]) dependencyTable.entrySet().stream().flatMap(entry ->
                Stream.concat(Stream.of(entry.getKey()), entry.getValue().keySet().stream())
        ).toArray();

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

    private static Result<Class<?>, String> getCreationPhase(Node node) {
        try {
            Method getNodeInfo = Node.class.getDeclaredMethod("getNodeInfo", Class.class);
            getNodeInfo.setAccessible(true);
            PhaseSourceNodeAnnotation source = (PhaseSourceNodeAnnotation) getNodeInfo.invoke(node, PhaseSourceNodeAnnotation.class);
            if (source != null)
                return Result.success(source.getSource());
            else
                return Result.error("Creation phase was null");

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            return Result.error("getCreationPhase failed:\n" + stringWriter.toString() + "\n");
        }
    }

    private static void setCreationPhase(Node node, Class<?> phaseClass) {
        try {
            Method setNodeInfo = Node.class.getDeclaredMethod("setNodeInfo", Class.class, Object.class);
            setNodeInfo.setAccessible(true);
            setNodeInfo.invoke(node, PhaseSourceNodeAnnotation.class, new PhaseSourceNodeAnnotation(phaseClass));

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
