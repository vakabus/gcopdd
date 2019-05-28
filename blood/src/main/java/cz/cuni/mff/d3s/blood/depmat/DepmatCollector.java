package cz.cuni.mff.d3s.blood.depmat;

import cz.cuni.mff.d3s.blood.report.TextDump;
import cz.cuni.mff.d3s.blood.phasestack.PhaseID;
import cz.cuni.mff.d3s.blood.utils.ConcurrentMatrix;
import cz.cuni.mff.d3s.blood.utils.ConcurrentOrderedSet;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.util.stream.Collectors;

public final class DepmatCollector implements TextDump {
    private static final NodeTracker nodeTracker = new NodeTracker();

    // the default of 16 doesn't fit even the most trivial programs
    private static final int HASHMAP_INIT_CAPACITY = 64;

    private final ConcurrentOrderedSet<PhaseID> phaseOrder = new ConcurrentOrderedSet<>();
    private final ConcurrentMatrix<PhaseID, PhaseID, DependencyValue> matrix = new ConcurrentMatrix<>(HASHMAP_INIT_CAPACITY, DependencyValue.ZERO);

    /**
     * This function is called by the instrumentation before every optimization
     * phase run. More specifically, before calling
     * {@link org.graalvm.compiler.phases.BasePhase#apply(StructuredGraph, Object)}
     *
     * @param graph       Graph entering the optimization phase
     * @param sourceClass Class of the optimization phase running
     */
    public void prePhase(StructuredGraph graph, Class<?> sourceClass) {
        var phaseID = getCurrentPhaseId(sourceClass);

        // obtain row in result matrix for this particular optimization phase
        var row = matrix.getOrCreateRow(phaseID);

        // for each node in the graph entering the phase, note down where it was created
        for (Node node : graph.getNodes()) {
            var creationPhaseResult = nodeTracker.getCreationPhase(node);

            if (creationPhaseResult.isError()) {
                System.err.println(creationPhaseResult.unwrapError());
                continue;
            }

            var creationPhaseClass = creationPhaseResult.unwrap();

            DependencyValue value = row.getOrCreate(creationPhaseClass);
            value.incrementNumberOfSeenNodes(1);
        }

        // update total node counts for all tracked values
        row.valuesStream().forEach(value -> {
            value.incrementTotalNumberOfNodesSeen(graph.getNodeCount());
            value.incrementPhaseCounter();
        });
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
        var phaseID = getCurrentPhaseId(sourceClass);
        nodeTracker.updateCreationPhase(graph.getNodes(), phaseID);
    }
    
    private PhaseID getCurrentPhaseId(Class<?> sourceClass) {
        return new PhaseID();
    }

    @Override
    public String getName() {
        return "depmat";
    }

    @Override
    public String getText() {
        String header = phaseOrder.stream()
                .map(PhaseID::toString)
                .collect(Collectors.joining("\n"));

        String data = matrix.toString(phaseOrder::stream, phaseOrder::stream);

        return header + "\n\n" + data;
    }
}