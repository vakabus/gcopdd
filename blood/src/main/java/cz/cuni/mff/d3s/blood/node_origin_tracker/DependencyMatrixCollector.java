package cz.cuni.mff.d3s.blood.node_origin_tracker;

import cz.cuni.mff.d3s.blood.report.Report;
import cz.cuni.mff.d3s.blood.report.dump.ManualTextDump;
import cz.cuni.mff.d3s.blood.utils.CheckedConsumer;
import org.graalvm.compiler.core.common.CompilationIdentifier;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DependencyMatrixCollector {

    // the default of 16 doesn't fit even the most trivial programs
    private static final int HASHMAP_INIT_CAPACITY = 64;

    private static DependencyMatrixCollector instance = null;
    /**
     * Multiple threads are writing to the result matrix at once. That's fine.
     * However, in the end, we want to dump the data and nobody should be
     * writing at that time. We can achieve this kind of locking by using
     * {@link ReadWriteLock} in the opposite way, than it was designed. Lock for
     * reading, when we are writing. Lock for writing, when we are reading.
     */
    private final ReadWriteLock writers = new ReentrantReadWriteLock(true);

    private final ConcurrentLinkedDeque<PhaseID> phaseOrder = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<PhaseID, ConcurrentHashMap<PhaseID, DependencyValue>> dependencyTable = new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY);
    // TODO make this configurable, also works with `new DefaultNodeTracker()`
    private final NodeTracker nodeTracker = new CustomNodeTracker();
    /**
     * getValue.apply(rowKey).apply(columnKey) returns DependencyValue. Returns
     * value from matrix. If it encounters a null on the way, returns
     * {@link DependencyValue#ZERO}.
     */
    private final Function<PhaseID, Function<PhaseID, DependencyValue>> getValue = p1 -> {
        final var row = dependencyTable.get(p1);
        return (row == null)
                ? p2 -> DependencyValue.ZERO
                : p2 -> row.getOrDefault(p2, DependencyValue.ZERO);
    };

    {
        phaseOrder.add(NodeTracker.NO_PHASE_DUMMY_PHASE_ID);
        phaseOrder.add(NodeTracker.DELETED_PHASE_DUMMY_PHASE_ID);
    }

    private DependencyMatrixCollector() {
        // register hook for dumping data
        Report.getInstance().registerDump(new ManualTextDump("depmat", this::dump));
    }

    public static DependencyMatrixCollector getInstance() {
        if (instance == null) {
            synchronized (DependencyMatrixCollector.class) {
                if (instance == null)
                    instance = new DependencyMatrixCollector();
            }
        }
        return instance;
    }

    /**
     * This function is called by the instrumentation after the Node class is
     * initialized.
     */
    public final void onNodeClassInit() {
        // initialize everything needed by the node tracker implementation
        nodeTracker.onNodeClassInit();
    }

    /**
     * This function is called by the instrumentation before every optimization
     * phase run. More specifically, before calling
     * {@link org.graalvm.compiler.phases.BasePhase#apply(StructuredGraph, Object)}
     *
     * @param graph       Graph entering the optimization phase
     * @param sourceClass Class of the optimization phase running
     */
    public final void prePhase(StructuredGraph graph, Class<?> sourceClass) {
        Lock readLock = writers.readLock();
        if (!readLock.tryLock()) {
            // Dump is already in progress, don't change the matrix any more.
            return;
        }

        var phaseID = getCurrentPhaseId(sourceClass);

        try {

            // obtain row in result matrix for this particular optimization phase
            var row = dependencyTable.computeIfAbsent(phaseID, p -> {
                phaseOrder.add(phaseID);
                return new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY);
            });

            // for each node in the graph entering the phase, note down where it was created
            for (Node node : graph.getNodes()) {
                var creationPhaseResult = nodeTracker.getCreationPhase(node);

                if (creationPhaseResult.isError()) {
                    System.err.println(creationPhaseResult.unwrapError());
                    continue;
                }

                var creationPhase = creationPhaseResult.unwrap();

                DependencyValue value = row.computeIfAbsent(creationPhase, c -> new DependencyValue());
                value.incrementNumberOfSeenNodes(1);
            }

            // update total node counts for all tracked values
            for (var value : row.values()) {
                value.incrementTotalNumberOfNodesSeen(graph.getNodeCount());
                value.incrementPhaseCounter();
            }

        } finally {
            // don't forget to unlock the lock
            readLock.unlock();
        }
    }

    /**
     * This function is called by the instrumentation after every optimization
     * phase run. More specifically, after calling
     * {@link org.graalvm.compiler.phases.BasePhase#apply(StructuredGraph, Object)}
     *
     * @param graph       Graph representing IL after being processed by the
     *                    optimization phase
     * @param sourceClass Class of the running optimization phase
     */
    public final void postPhase(StructuredGraph graph, Class<?> sourceClass) {
        var phaseID = getCurrentPhaseId(sourceClass);
        nodeTracker.updateCreationPhase(graph.getNodes(), phaseID);
    }

    private PhaseID getCurrentPhaseId(Class<?> sourceClass) {
        return new PhaseID(sourceClass);
    }

    /**
     * Method called on JVM exit dumping collected statistics.
     */
    private String dump() {
        // Block, so that nobody can write to the matrix.
        Lock writeLock = writers.writeLock();
        writeLock.lock();
        StringWriter out = new StringWriter();
        try {

            // collect all phase classes
            final PhaseID[] keysOrder = phaseOrder.toArray(i -> new PhaseID[i]);

            // List of classes in the order that is used in the matrix below.
            // Each line contains space-separated list of the class's
            // superclasses from itself up to java.lang.Object.
            Arrays.stream(keysOrder).map(clazz -> clazz.toString() + "\n")
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

        } finally {
            writeLock.unlock();
        }

        return out.toString();
    }
}
