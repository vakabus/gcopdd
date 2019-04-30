package cz.cuni.mff.d3s.blood.node_type_tracker;

import cz.cuni.mff.d3s.blood.report.Report;
import cz.cuni.mff.d3s.blood.report.dump.ManualTextDump;
import cz.cuni.mff.d3s.blood.utils.ConcurrentMatrix;
import cz.cuni.mff.d3s.blood.utils.ConcurrentOrderedSet;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class NodeTypeTrackerCollector {

    // the default of 16 doesn't fit even the most trivial programs
    private static final int HASHMAP_INIT_CAPACITY = 64;

    private static NodeTypeTrackerCollector instance = null;

    private final ReadWriteLock writers = new ReentrantReadWriteLock(true);

    private final ConcurrentOrderedSet<Class> nodeClasses = new ConcurrentOrderedSet<>();
    private final ConcurrentOrderedSet<Class> phaseClasses = new ConcurrentOrderedSet<>();

    private final ConcurrentMatrix<Class, Class, NodeTrackerValue> prePhaseTypes = new ConcurrentMatrix<>(HASHMAP_INIT_CAPACITY, NodeTrackerValue.ZERO);
    private final ConcurrentMatrix<Class, Class, NodeTrackerValue> postPhaseTypes = new ConcurrentMatrix<>(HASHMAP_INIT_CAPACITY, NodeTrackerValue.ZERO);

    private NodeTypeTrackerCollector() {
        Report.getInstance().registerDump(new ManualTextDump("nodetypematrix", this::dump));
    }

    public static NodeTypeTrackerCollector getInstance() {
        if (instance == null) {
            synchronized (NodeTypeTrackerCollector.class) {
                if (instance == null) {
                    instance = new NodeTypeTrackerCollector();
                }
            }
        }
        return instance;
    }

    public final void updateRow(StructuredGraph graph, ConcurrentMatrix<Class, Class, NodeTrackerValue>.Row row) {
        HashMap<Class, Long> nodeCount = new HashMap<>();
        for (Node n : graph.getNodes()) {
            Long count = nodeCount.getOrDefault(n.getClass(), 0l);
            count++;
            nodeCount.put(n.getClass(), count);
        }

        for (Map.Entry<Class, Long> classLongEntry : nodeCount.entrySet()) {
            Class nodeClass = classLongEntry.getKey();

            nodeClasses.add(nodeClass);

            row.getOrCreate(nodeClass)
                    .incrementNumberOfSeenNodes(classLongEntry.getValue());
        }

        final long totalCount = graph.getNodeCount();
        row.valuesStream().forEach(nodeTrackerValue -> nodeTrackerValue.incrementTotalNumberOfNodesSeen(totalCount));
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
        var row = prePhaseTypes.getOrCreateRow(sourceClass);
        phaseClasses.add(sourceClass);
        updateRow(graph, row);
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
        var row = postPhaseTypes.getOrCreateRow(sourceClass);
        updateRow(graph, row);
    }

    /**
     * Method called on JVM exit dumping collected statistics.
     */
    private String dump() {
        // Block, so that nobody can write to the matrix.
        Lock writeLock = writers.writeLock();
        writeLock.lock();
        try {
            String nodeClassesStr = nodeClasses.stream()
                    .map(Class::getName)
                    .collect(Collectors.joining("\n"));

            String phaseClassesStr = phaseClasses.stream()
                    .map(Class::getName)
                    .collect(Collectors.joining("\n"));

            String prePhaseStr = prePhaseTypes.toString(phaseClasses::stream, nodeClasses::stream);

            String postPhaseStr = postPhaseTypes.toString(phaseClasses::stream, nodeClasses::stream);

            return nodeClassesStr + "\n\n" + phaseClassesStr + "\n\n" + prePhaseStr + "\n\n" + postPhaseStr;
        } finally {
            writeLock.unlock();
        }
    }
}
