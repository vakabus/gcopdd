package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BodyMarker;
import cz.cuni.mff.d3s.blood.report.Manager;
import org.graalvm.compiler.core.common.CompilationIdentifier;
import org.graalvm.compiler.nodes.StructuredGraph;

public class CompilationTrackingInject {

    @Before(marker = BodyMarker.class, scope = "void GraalCompiler.emitFrontEnd(*,*,*,*,*,*,*)")
    public static void markNewCompilationStart(DynamicContext di) {
        StructuredGraph graph = di.getMethodArgumentValue(2, StructuredGraph.class);
        var id = graph.compilationId();

        String identifier = id.toString(CompilationIdentifier.Verbosity.DETAILED);
        Manager.markCompilationStart(identifier);
    }

    @After(marker = BodyMarker.class, scope = "void GraalCompiler.emitFrontEnd(*,*,*,*,*,*,*)")
    public static void markNewCompilationEnd(DynamicContext di) {
        StructuredGraph graph = di.getMethodArgumentValue(2, StructuredGraph.class);
        var id = graph.compilationId();

        String identifier = id.toString(CompilationIdentifier.Verbosity.DETAILED);
        Manager.markCompilationEnd(identifier);
    }
}
