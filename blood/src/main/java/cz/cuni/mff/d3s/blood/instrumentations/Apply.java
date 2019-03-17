package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BodyMarker;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class Apply {


    /*@Before(marker = BodyMarker.class, scope = "void BasePhase.apply(org.graalvm.compiler.nodes.StructuredGraph, *)")
    public static void beforeApply(ArgumentProcessorContext apc) {
        //System.err.println(repr(apc.getArgs(ArgumentProcessorMode.METHOD_ARGS)));
    }*/

    public static String repr(Object o) {
        if (o == null)
            return "<null>";
        if (o instanceof String)
            return "\"" + o + "\"";
        if (o instanceof Object[])
            return stream((Object[]) o)
                    .map(Apply::repr)
                    .collect(Collectors.joining(", ", "[", "]"));
        return o.toString();
    }

    public static Set<StructuredGraph> GRAPHS = new HashSet<>();


    @Before(marker = BodyMarker.class, scope = "void BasePhase.apply(org.graalvm.compiler.nodes.StructuredGraph, *)")
    public static void beforePhaseRun(DynamicContext di) {
        Object thiz = di.getThis();
        StructuredGraph graph = di.getMethodArgumentValue(0, StructuredGraph.class);

        System.err.printf("%-80s uses Graph which was %s before\n", String.format("[ %s ]", thiz.getClass().getCanonicalName()), GRAPHS.contains(graph) ? "  SEEN  " : "NOT SEEN");
        GRAPHS.add(graph);
    }
}
