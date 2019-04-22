package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BodyMarker;
import cz.cuni.mff.d3s.blood.node_origin_tracker.DependencyMatrixCollector;
import org.graalvm.compiler.nodes.StructuredGraph;

/**
 *  Tracks nodes in IL, saves their creation phase. Collects statistics, where were nodes created, when they enter any
 *  optimization phase. You can then ask, given a one phase, which other phase produces most of the nodes that the first
 *  phase operates on.
 */
public class DependencyMatrixCollectorInject {

    @After(marker = BodyMarker.class, scope = "void Node.<clinit>()")
    public static void afterNodeClinit() {
        DependencyMatrixCollector.getInstance().onNodeClassInit();
    }

    @Before(marker = BodyMarker.class, scope = "void BasePhase.apply(org.graalvm.compiler.nodes.StructuredGraph, *)")
    public static void beforePhaseRun(DynamicContext di) {
        Object thiz = di.getThis();
        StructuredGraph graph = di.getMethodArgumentValue(0, StructuredGraph.class);

        DependencyMatrixCollector.getInstance().prePhase(graph, thiz.getClass());
    }

    @After(marker = BodyMarker.class, scope = "void BasePhase.apply(org.graalvm.compiler.nodes.StructuredGraph, *)")
    public static void afterPhaseRun(DynamicContext di) {
        Object thiz = di.getThis();
        StructuredGraph graph = di.getMethodArgumentValue(0, StructuredGraph.class);

        DependencyMatrixCollector.getInstance().postPhase(graph, thiz.getClass());
    }
}
