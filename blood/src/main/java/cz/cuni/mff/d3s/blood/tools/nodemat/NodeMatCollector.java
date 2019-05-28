package cz.cuni.mff.d3s.blood.tools.nodemat;

import cz.cuni.mff.d3s.blood.report.TextDump;
import cz.cuni.mff.d3s.blood.utils.matrix.Matrix;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class NodeMatCollector implements TextDump {
    // the default of 16 doesn't fit even the most trivial programs
    private static final int HASHMAP_INIT_CAPACITY = 64;

    private final Matrix<Class, Class, NodeTrackerValue> preMatrix = new Matrix<>(HASHMAP_INIT_CAPACITY, NodeTrackerValue.ZERO);
    private final Matrix<Class, Class, NodeTrackerValue> postMatrix = new Matrix<>(HASHMAP_INIT_CAPACITY, NodeTrackerValue.ZERO);

    private final LinkedHashSet<Class> nodeClasses = new LinkedHashSet<>();
    private final LinkedHashSet<Class> phaseClasses = new LinkedHashSet<>();


    /**
     * This function is called by the instrumentation before every optimization
     * phase run. More specifically, before calling
     * {@link org.graalvm.compiler.phases.BasePhase#apply(StructuredGraph, Object)}
     *
     * @param graph       Graph entering the optimization phase
     * @param sourceClass Class of the optimization phase running
     */
    public void prePhase(StructuredGraph graph, Class<?> sourceClass) {
        update(graph, sourceClass, preMatrix);
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
    public void postPhase(StructuredGraph graph, Class<?> sourceClass) {
        update(graph, sourceClass, postMatrix);
    }

    private void update(StructuredGraph graph, Class phaseClass, Matrix<Class, Class, NodeTrackerValue> matrix) {
        phaseClasses.add(phaseClass);

        updateRow(graph, matrix.getOrCreateRow(phaseClass));
    }

    private void updateRow(StructuredGraph graph, Matrix<Class, Class, NodeTrackerValue>.Row row) {
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
        row.valuesStream().forEach(NodeTrackerValue::incrementPhaseCounter);
    }

    @Override
    public String getName() {
        return "nodemat";
    }

    @Override
    public String getText() {
        String nodeClassesStr = nodeClasses.stream()
                .map(Class::getName)
                .collect(Collectors.joining("\n"));

        String phaseClassesStr = phaseClasses.stream()
                .map(Class::getName)
                .collect(Collectors.joining("\n"));

        String prePhaseStr = preMatrix.toString(phaseClasses::stream, nodeClasses::stream);
        String postPhaseStr = postMatrix.toString(phaseClasses::stream, nodeClasses::stream);

        return nodeClassesStr + "\n\n" + phaseClassesStr + "\n\n" + prePhaseStr + "\n\n" + postPhaseStr;
    }
}
