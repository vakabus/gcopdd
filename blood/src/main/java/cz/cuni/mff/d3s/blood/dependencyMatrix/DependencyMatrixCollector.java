package cz.cuni.mff.d3s.blood.dependencyMatrix;

import cz.cuni.mff.d3s.blood.utils.Result;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public final class DependencyMatrixCollector {
    static ConcurrentHashMap<Class, ConcurrentHashMap<Class, DependencyValue>> dependencyTable = new ConcurrentHashMap<>();

    public static void init() {
        // make sure, that Graal will track node creation position
        var props = System.getProperties();
        props.setProperty("debug.graal.TrackNodeCreationPosition", "true");
    }

    private static Result<StackTraceElement[], String> getCreationStackTrace(Node node) {
        Object a = node.getCreationPosition();
        if (a == null) {
            return Result.error("Node creation position is null!");
        }

        try {
            Class<?> aClass = a.getClass();
            Field f = aClass.getField("stackTrace");
            f.setAccessible(true);
            StackTraceElement[] stackTrace = new StackTraceElement[0];
            stackTrace = (StackTraceElement[]) f.get(a);
            return Result.success(stackTrace);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            return Result.error("getCreationStackTrace failed:\n" + stringWriter.toString() + "\n");
        }
    }

    public static void prePhase(StructuredGraph graph, Class<?> sourceClass) {
        var row = dependencyTable.get(sourceClass);

        try {
            for (var node : graph.getNodes()) {
                Result<StackTraceElement[], String> traceResult = getCreationStackTrace(node);

                if (traceResult.isOk()) {
                    Arrays.stream(traceResult.unwrap()).forEachOrdered(stackTraceElement -> System.out.println(stackTraceElement.getMethodName()));
                    System.out.println();
                } else {
                    System.out.println(traceResult.unwrapError());
                }
            }

        } catch (Throwable e) {
            // FIXME
            // this syncblock is here to serialize output
            synchronized (DependencyMatrixCollector.class) {
                System.out.println("prePhase function failed in DependencyMatrixCollector due to:");
                e.printStackTrace();
                System.out.println();
            }
        }
    }

    public static void postPhase(StructuredGraph graph, Class<?> sourceClass) {
        // here we wanted to mark every created node as created here
        // currently, that's unnecessary, because we are using Graal's
        // own creation tracking
    }
}
