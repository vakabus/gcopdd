package cz.cuni.mff.d3s.blood.tools.phasestack;

import cz.cuni.mff.d3s.blood.report.Manager;

public class PhaseID {

    public static final PhaseID NO_PHASE = new PhaseID(-2);
    public final int id;

    public static PhaseID getCurrent() {
        return new PhaseID(Manager.get(PhaseStackCollector.class).getStackStateID());
    }

    private PhaseID(int id) {
        this.id = id;
    }
}
