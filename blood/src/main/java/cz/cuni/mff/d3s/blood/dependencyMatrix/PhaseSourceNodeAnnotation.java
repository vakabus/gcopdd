package cz.cuni.mff.d3s.blood.dependencyMatrix;

public final class PhaseSourceNodeAnnotation {
    public PhaseSourceNodeAnnotation(Class<?> source) {
        this.source = source;
    }

    public Class<?> getSource() {
        return source;
    }

    final Class<?> source;
}
