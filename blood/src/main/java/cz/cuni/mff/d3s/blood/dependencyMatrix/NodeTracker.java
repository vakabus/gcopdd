package cz.cuni.mff.d3s.blood.dependencyMatrix;

import cz.cuni.mff.d3s.blood.utils.Result;
import org.graalvm.compiler.graph.Node;

/**
 * Objects implementing this interface are capable of tracking nodes between
 * compilation phases.
 */
public interface NodeTracker {

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
    Result<Class<?>, String> getCreationPhase(Node node);

    /**
     * Should be called after every phase.
     *
     * @param nodes list of nodes in the graph
     * @param phaseClass class of the phase in question
     */
    void updateCreationPhase(Iterable<Node> nodes, Class<?> phaseClass);

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
