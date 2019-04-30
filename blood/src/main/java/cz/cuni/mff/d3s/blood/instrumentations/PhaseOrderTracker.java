package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BodyMarker;
import cz.cuni.mff.d3s.blood.instrumentations.marker.ApplyInvocationMarker;
import cz.cuni.mff.d3s.blood.phase_order_tracker.PhaseOrder;

public class PhaseOrderTracker {
    @Before(marker = BodyMarker.class, scope = "void GraalCompiler.emitFrontEnd(*,*,*,*,*,*,*)")
    public static void resetCounter() {
        PhaseOrder.resetCounter();
    }

    @After(marker = ApplyInvocationMarker.class, scope = "void org.graalvm.compiler.phases.PhaseSuite.run(StructuredGraph, *)")
    public static void afterPhaseRunBB(){
        PhaseOrder.incrementCounter();
    }
}
