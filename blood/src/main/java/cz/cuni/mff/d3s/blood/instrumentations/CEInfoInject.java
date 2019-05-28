package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BodyMarker;
import cz.cuni.mff.d3s.blood.report.Manager;
import cz.cuni.mff.d3s.blood.tools.ceinfo.CEInfoCollector;

public class CEInfoInject {
    
    @Before(marker = BodyMarker.class, scope = "CompilationRequestResult HotSpotGraalCompiler.compileMethod(CompilationRequest)")
    public static void beforeCompileMethod(DynamicContext di) {
        Manager.get(CEInfoCollector.class).beforeCompileMethod(di.getMethodArgumentValue(0, Object.class));
    }

    @After(marker = BodyMarker.class, scope = "CompilationRequestResult HotSpotGraalCompiler.compileMethod(CompilationRequest)")
    public static void afterCompileMethod(DynamicContext di) {
        Manager.get(CEInfoCollector.class).afterCompileMethod(di.getMethodArgumentValue(0, Object.class));
    }
}
