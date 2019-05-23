package cz.cuni.mff.d3s.blood.node_origin_tracker;

import cz.cuni.mff.d3s.blood.utils.Result;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.graalvm.compiler.graph.Node;

/**
 * Node tracker that does not use the node tracking facilities built into Graal.
 */
public class CustomNodeTracker implements NodeTracker {

    private Method getNodeInfo, setNodeInfo;

    @Override
    public void onNodeClassInit() {
        try {
            getNodeInfo = Node.class.getDeclaredMethod("getNodeInfo", Class.class);
            setNodeInfo = Node.class.getDeclaredMethod("setNodeInfo", Class.class, Object.class);
            getNodeInfo.setAccessible(true);
            setNodeInfo.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Result<Class<?>, String> getCreationPhase(Node node) {
        try {
            PhaseSourceNodeAnnotation source = (PhaseSourceNodeAnnotation) getNodeInfo.invoke(node, PhaseSourceNodeAnnotation.class);
            return Result.success(source != null ? source.getSource() : NodeTracker.NoPhaseDummy.class);
        } catch (IllegalAccessException | InvocationTargetException e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            return Result.error("getCreationPhase failed:\n" + stringWriter.toString() + "\n");
        }
    }

    public void setCreationPhase(Node node, Class<?> phaseID) {
        try {
            setNodeInfo.invoke(node, PhaseSourceNodeAnnotation.class, new PhaseSourceNodeAnnotation(phaseID));
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateCreationPhase(Iterable<Node> nodes, Class<?> phaseID) {
        // mark all nodes without any creation annotation as created in this phase
        for (Node node : nodes) {
            var creationPhase = getCreationPhase(node);
            if (creationPhase.isError()) {
                System.err.println(creationPhase.unwrapError());
            } else if (creationPhase.unwrap() == NoPhaseDummy.class) {
                setCreationPhase(node, phaseID);
            }
        }
    }
}
