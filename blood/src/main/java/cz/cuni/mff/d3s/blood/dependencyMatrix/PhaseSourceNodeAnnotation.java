package cz.cuni.mff.d3s.blood.dependencyMatrix;

public final class PhaseSourceNodeAnnotation {

    final Class<?> source;

    public PhaseSourceNodeAnnotation(Class<?> source) {
        this.source = source;
    }

    public Class<?> getSource() {
        return source;
    }
}
