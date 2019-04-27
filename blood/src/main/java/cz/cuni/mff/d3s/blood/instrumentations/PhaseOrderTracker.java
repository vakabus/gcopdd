package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BasicBlockMarker;
import ch.usi.dag.disl.marker.BodyMarker;
import cz.cuni.mff.d3s.blood.phase_order_tracker.PhaseOrder;
import org.graalvm.compiler.phases.PhaseSuite;

public class PhaseOrderTracker {
    @Before(marker = BodyMarker.class, scope = "CompilationRequestResult HotSpotGraalCompiler.compileMethod(*)")
    public static void resetCounter() {
        PhaseOrder.resetCounter();
    }

    @After(marker = BasicBlockMarker.class, scope = "void org.graalvm.compiler.phases.PhaseSuite.run(StructuredGraph, *)")
    public static void afterPhaseRunBB(){
        PhaseOrder.incrementCounter();
    }
}
