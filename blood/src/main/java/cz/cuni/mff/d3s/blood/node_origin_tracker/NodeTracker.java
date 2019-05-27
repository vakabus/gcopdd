package cz.cuni.mff.d3s.blood.node_origin_tracker;

import cz.cuni.mff.d3s.blood.phaseid.PhaseID;
import cz.cuni.mff.d3s.blood.phaseid.PhaseOrderPhaseID;
import cz.cuni.mff.d3s.blood.utils.Result;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.phases.BasePhase;

/**
 * Objects implementing this interface are capable of tracking nodes between
 * compilation phases.
 */
public interface NodeTracker {

    public static final PhaseID DELETED_PHASE_DUMMY_PHASE_ID = new PhaseOrderPhaseID(DeletedPhaseDummy.class, 0);
    public static final PhaseID NO_PHASE_DUMMY_PHASE_ID = new PhaseOrderPhaseID(NoPhaseDummy.class, 0);

    /**
     * Should be called right after the {@link Node} class is initialized.
     */
    void onNodeClassInit();

    /**
     * Get the class of the phase, in which given node was created.
     *
     * @param node the node in question
     * @return class of the phase, or a dummy, or an error message
     */
    Result<PhaseID, String> getCreationPhase(Node node);

    /**
     * Should be called after every phase.
     *
     * @param nodes   list of nodes in the graph
     * @param phaseID id of the phase in question
     */
    void updateCreationPhase(Iterable<Node> nodes, PhaseID phaseID);

    /**
     * Replaces phase class in cases, where the node creation stack trace was
     * explicitly deleted by Graal.
     */
    public static class DeletedPhaseDummy {
    }

    /**
     * Replaces phase class in cases, where the node creation stack trace did
     * not contain any phase class (ie subclass of {@link BasePhase}).
     */
    public static class NoPhaseDummy {
    }
}
