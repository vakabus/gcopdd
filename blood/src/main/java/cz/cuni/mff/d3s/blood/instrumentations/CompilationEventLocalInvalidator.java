package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BodyMarker;
import cz.cuni.mff.d3s.blood.method_local.CompilationEventLocal;

/**
 * Enables or disables per compilation event data collection. When not activated, all data will be aggregated. Otherwise
 * some of it will be collected and dumped per compilation event.
 */
public class CompilationEventLocalInvalidator {

    /**
     * We need to flip one boolean marking, whether CompilationEventLocal feature is enabled, before running the constructor
     * for the first time. We can't instrument ourselves, so that's why we instrument something, that get's loaded as
     * early as possible.
     */
    @After(marker = BodyMarker.class, scope = "void HotSpotGraalCompiler.<clinit>()")
    public static void enableCompilationEventLocalFeature() {
        CompilationEventLocal.enableFeature();
    }


    @Before(marker = BodyMarker.class, scope = "void GraalCompiler.emitFrontEnd(*,*,*,*,*,*,*)")
    public static void resetCompilationEventLocals() {
        CompilationEventLocal.markNewCompilation();
    }
}
