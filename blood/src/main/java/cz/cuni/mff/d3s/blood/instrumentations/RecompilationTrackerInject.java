package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BodyMarker;
import cz.cuni.mff.d3s.blood.recompilation_tracker.RecompilationTracker;
import org.graalvm.compiler.nodes.StructuredGraph;

public class RecompilationTrackerInject {
    
    @Before(marker = BodyMarker.class, scope = "CompilationRequestResult HotSpotGraalCompiler.compileMethod(CompilationRequest)")
    public static void beforeCompileMethod(DynamicContext di) {
        RecompilationTracker.getInstance().beforeCompileMethod(di.getMethodArgumentValue(0, Object.class));
    }

    @After(marker = BodyMarker.class, scope = "CompilationRequestResult HotSpotGraalCompiler.compileMethod(CompilationRequest)")
    public static void afterCompileMethod(DynamicContext di) {
        RecompilationTracker.getInstance().afterCompileMethod(di.getMethodArgumentValue(0, Object.class));
    }
    
    @Before(marker = BodyMarker.class, scope = "void BasePhase.apply(org.graalvm.compiler.nodes.StructuredGraph, *)")
    public static void beforePhaseRun(DynamicContext di) {
        StructuredGraph graph = di.getMethodArgumentValue(0, StructuredGraph.class);

        RecompilationTracker.getInstance().prePhase(graph);
    }
}
