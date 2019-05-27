package cz.cuni.mff.d3s.blood.phaseid;

import cz.cuni.mff.d3s.blood.phase_stack_tracker.PhaseStackTracker;

public class PhaseStackPhaseID {
    public final int stackStateID;

    public PhaseStackPhaseID() {
        this.stackStateID = PhaseStackTracker.getInstance().getStackStateID();
    }
}
