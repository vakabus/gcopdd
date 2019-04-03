package cz.cuni.mff.d3s.blood.dependencyMatrix;

import org.graalvm.compiler.nodes.StructuredGraph;

import java.util.concurrent.ConcurrentHashMap;

public final class DependencyMatrixCollector {
    static ConcurrentHashMap<Class, ConcurrentHashMap<Class, DependencyValue>> dependencyTable = new ConcurrentHashMap<>();

    public static void prePhase(StructuredGraph graph, Class<?> sourceClass) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static void postPhase(StructuredGraph graph, Class<?> sourceClass) {
        throw new UnsupportedOperationException("not implemented");
    }
}
