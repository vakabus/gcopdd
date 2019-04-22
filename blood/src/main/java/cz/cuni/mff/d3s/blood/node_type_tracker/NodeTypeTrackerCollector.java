package cz.cuni.mff.d3s.blood.node_type_tracker;

import cz.cuni.mff.d3s.blood.report.Report;
import cz.cuni.mff.d3s.blood.report.dump.ManualDump;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeTypeTrackerCollector {
    // the default of 16 doesn't fit even the most trivial programs
    private static final int HASHMAP_INIT_CAPACITY = 64;

    private static NodeTypeTrackerCollector instance = null;

    private final ConcurrentHashMap<Class, ConcurrentHashMap<Class, NodeTrackerValue>> postPhaseTypes = new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY);
    private final ConcurrentHashMap<Class, ConcurrentHashMap<Class, NodeTrackerValue>> prePhaseTypes = new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY);

    public NodeTypeTrackerCollector() {
        // FIXME implement dumping
        Report.getInstance().registerDump(new ManualDump("nodetypematrix", () -> new byte[0]));
    }

    public static NodeTypeTrackerCollector getInstance() {
        if (instance == null) {
            instance = new NodeTypeTrackerCollector();
        }
        return instance;
    }

    public final void updateNodeTypes(StructuredGraph graph, ConcurrentHashMap<Class, NodeTrackerValue> phaseTable) {
        HashMap<Class, Long> nodeCount = new HashMap<>();
        for (Node n : graph.getNodes()) {
            Long count = nodeCount.getOrDefault(n.getClass(), 0l);
            count++;
            nodeCount.put(n.getClass(), count);
        }

        for (Map.Entry<Class, Long> classLongEntry : nodeCount.entrySet()) {
            Class nodeClass = classLongEntry.getKey();

            phaseTable.computeIfAbsent(nodeClass, aClass -> new NodeTrackerValue())
                    .incrementNumberOfSeenNodes(classLongEntry.getValue());
        }

        final long totalCount = graph.getNodeCount();
        phaseTable.values().stream().forEach(nodeTrackerValue -> nodeTrackerValue.incrementTotalNumberOfNodesSeen(totalCount));
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
        ConcurrentHashMap<Class, NodeTrackerValue> row = prePhaseTypes.computeIfAbsent(sourceClass, aClass -> new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY));
        updateNodeTypes(graph, row);
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
        ConcurrentHashMap<Class, NodeTrackerValue> row = postPhaseTypes.computeIfAbsent(sourceClass, aClass -> new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY));
        updateNodeTypes(graph, row);
    }
}
