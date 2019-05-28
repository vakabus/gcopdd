package cz.cuni.mff.d3s.blood.phaseid;

import cz.cuni.mff.d3s.blood.phase_stack_tracker.PhaseStackTracker;

public class PhaseOrderID implements PhaseID{
    public final int stackStateID;

    private PhaseOrderID(int order) {
        this.stackStateID = order;
    }

    public static PhaseOrderID getCurrentPhaseOrderID() {
        return new PhaseOrderID(PhaseStackTracker.getCurrentPhaseNumber());
    }

    @Override
    public String toString() {
        return String.format("#%d", stackStateID);
    }
}
