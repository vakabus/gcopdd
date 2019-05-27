package cz.cuni.mff.d3s.blood.node_origin_tracker;

import cz.cuni.mff.d3s.blood.phaseid.PhaseID;

public final class PhaseSourceNodeAnnotation {

    final PhaseID source;

    public PhaseSourceNodeAnnotation(PhaseID source) {
        this.source = source;
    }

    public PhaseID getSource() {
        return source;
    }
}
