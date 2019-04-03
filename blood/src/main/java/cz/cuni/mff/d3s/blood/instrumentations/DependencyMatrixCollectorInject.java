package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BodyMarker;
import cz.cuni.mff.d3s.blood.dependencyMatrix.DependencyMatrixCollector;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.util.concurrent.atomic.AtomicBoolean;

public class DependencyMatrixCollectorInject {

    public final static AtomicBoolean initialized = new AtomicBoolean(false);

    @Before(marker = BodyMarker.class, scope = "* HotSpotGraalCompilerFactory.createCompiler(*)")
    public static void initialize() {
        if (initialized.getAndSet(true)) {
            DependencyMatrixCollector.init();
        } else {
            System.getLogger(DependencyMatrixCollectorInject.class.getName())
                    .log(System.Logger.Level.WARNING, "Already initialized!");
        }
    }

    @Before(marker = BodyMarker.class, scope = "void BasePhase.apply(org.graalvm.compiler.nodes.StructuredGraph, *)")
    public static void beforePhaseRun(DynamicContext di) {
        Object thiz = di.getThis();
        StructuredGraph graph = di.getMethodArgumentValue(0, StructuredGraph.class);

        DependencyMatrixCollector.prePhase(graph, thiz.getClass());
    }

    @After(marker = BodyMarker.class, scope = "void BasePhase.apply(org.graalvm.compiler.nodes.StructuredGraph, *)")
    public static void afterPhaseRun(DynamicContext di) {
        Object thiz = di.getThis();
        StructuredGraph graph = di.getMethodArgumentValue(0, StructuredGraph.class);

        DependencyMatrixCollector.postPhase(graph, thiz.getClass());
    }
}
