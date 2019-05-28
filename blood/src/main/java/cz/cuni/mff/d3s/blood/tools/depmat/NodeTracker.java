package cz.cuni.mff.d3s.blood.tools.depmat;

import cz.cuni.mff.d3s.blood.tools.phasestack.PhaseID;
import cz.cuni.mff.d3s.blood.utils.Result;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.graalvm.compiler.graph.Node;

/**
 * Node tracker that does not use the node tracking facilities built into Graal.
 */
@SuppressWarnings("StaticNonFinalUsedInInitialization")
public class NodeTracker {

    private static Method getNodeInfo, setNodeInfo;

    static {
        try {
            getNodeInfo = Node.class.getDeclaredMethod("getNodeInfo", Class.class);
            setNodeInfo = Node.class.getDeclaredMethod("setNodeInfo", Class.class, Object.class);
            getNodeInfo.setAccessible(true);
            setNodeInfo.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the class of the phase, in which given node was created.
     *
     * @param node the node in question
     * @return class of the phase, or a dummy, or an error message
     */
    public Result<PhaseID, String> getCreationPhase(Node node) {
        try {
            PhaseID source = (PhaseID) getNodeInfo.invoke(node, PhaseID.class);
            return Result.success(source != null ? source : PhaseID.NO_PHASE);
        } catch (IllegalAccessException | InvocationTargetException e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            return Result.error("getCreationPhase failed:\n" + stringWriter.toString() + "\n");
        }
    }

    public void setCreationPhase(Node node, PhaseID phaseID) {
        try {
            setNodeInfo.invoke(node, PhaseID.class, phaseID);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Should be called after every phase.
     *
     * @param nodes   list of nodes in the graph
     * @param phaseID id of the phase in question
     */
    public void updateCreationPhase(Iterable<Node> nodes, PhaseID phaseID) {
        // mark all nodes without any creation annotation as created in this phase
        for (Node node : nodes) {
            var creationPhase = getCreationPhase(node);
            if (creationPhase.isError()) {
                System.err.println(creationPhase.unwrapError());
            } else if (creationPhase.unwrap() == PhaseID.NO_PHASE) {
                setCreationPhase(node, phaseID);
            }
        }
    }
}
