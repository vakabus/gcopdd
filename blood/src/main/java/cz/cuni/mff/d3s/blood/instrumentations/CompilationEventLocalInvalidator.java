package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BodyMarker;
import cz.cuni.mff.d3s.blood.method_local.CompilationEventLocal;

public class CompilationEventLocalInvalidator {
    @Before(marker = BodyMarker.class, scope = "void GraalCompiler.emitFrontEnd(*,*,*,*,*,*,*)")
    public static void resetCompilationEventLocals() {
        CompilationEventLocal.markNewCompilation();
    }
}
