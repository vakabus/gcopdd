package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.util.Assert;
import cz.cuni.mff.d3s.blood.report.Manager;
import cz.cuni.mff.d3s.blood.utils.Miscellaneous;
import org.graalvm.compiler.core.common.CompilationIdentifier;
import org.graalvm.compiler.core.common.CompilationRequestIdentifier;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.lang.reflect.InvocationTargetException;

public class MarkNewCompilationInject {

    @Before(marker = BodyMarker.class, scope = "void GraalCompiler.emitFrontEnd(*,*,*,*,*,*,*)")
    public static void markNewCompilation(DynamicContext di) {
        StructuredGraph graph = di.getMethodArgumentValue(2, StructuredGraph.class);
        var id = graph.compilationId();

        String identifier = id.toString(CompilationIdentifier.Verbosity.DETAILED);
        Manager.markNewCompilation(identifier);
    }
}
