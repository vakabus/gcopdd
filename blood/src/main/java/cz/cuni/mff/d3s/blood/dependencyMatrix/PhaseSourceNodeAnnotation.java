package cz.cuni.mff.d3s.blood.dependencyMatrix;

public final class PhaseSourceNodeAnnotation {

    final PhaseID source;

    public PhaseSourceNodeAnnotation(PhaseID source) {
        this.source = source;
    }

    public PhaseID getSource() {
        return source;
    }
}
