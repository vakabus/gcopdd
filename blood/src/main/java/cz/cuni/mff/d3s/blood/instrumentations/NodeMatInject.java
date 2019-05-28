package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BodyMarker;
import cz.cuni.mff.d3s.blood.report.Manager;
import cz.cuni.mff.d3s.blood.tools.nodemat.NodeMatCollector;
import org.graalvm.compiler.nodes.StructuredGraph;

/**
 * Tracks types of nodes used in phases. Can answer questions like "Which node type is appearing the most in this phase?"
 */
public class NodeMatInject {
    @Before(marker = BodyMarker.class, scope = "void BasePhase.apply(org.graalvm.compiler.nodes.StructuredGraph, *)")
    public static void beforePhaseRun(DynamicContext di) {
        Object thiz = di.getThis();
        StructuredGraph graph = di.getMethodArgumentValue(0, StructuredGraph.class);

        Manager.get(NodeMatCollector.class).prePhase(graph, thiz.getClass());
    }

    @After(marker = BodyMarker.class, scope = "void BasePhase.apply(org.graalvm.compiler.nodes.StructuredGraph, *)")
    public static void afterPhaseRun(DynamicContext di) {
        Object thiz = di.getThis();
        StructuredGraph graph = di.getMethodArgumentValue(0, StructuredGraph.class);

        Manager.get(NodeMatCollector.class).postPhase(graph, thiz.getClass());
    }
}
