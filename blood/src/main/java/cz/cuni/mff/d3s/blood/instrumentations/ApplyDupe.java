package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.processorcontext.ArgumentProcessorContext;
import ch.usi.dag.disl.processorcontext.ArgumentProcessorMode;

import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class ApplyDupe {


    @Before(marker = BodyMarker.class, scope = "void BasePhase.apply(org.graalvm.compiler.nodes.StructuredGraph, *)")
    public static void beforeApply(ArgumentProcessorContext apc) {
        System.err.println(repr(apc.getArgs(ArgumentProcessorMode.METHOD_ARGS)));
    }

    public static String repr(Object o) {
        if (o == null)
            return "<null>";
        if (o instanceof String)
            return "\"" + o + "\"";
        if (o instanceof Object[])
            return stream((Object[]) o)
                    .map(ApplyDupe::repr)
                    .collect(Collectors.joining(", ", "[", "]"));
        return o.toString();
    }

}
