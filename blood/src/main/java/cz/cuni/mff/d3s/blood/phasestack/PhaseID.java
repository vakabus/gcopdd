package cz.cuni.mff.d3s.blood.phasestack;

import cz.cuni.mff.d3s.blood.report.Manager;
import cz.cuni.mff.d3s.blood.phasestack.PhasestackCollector;

public class PhaseID {

    public static final PhaseID NO_PHASE = new PhaseID(-2);
    public final int id;

    public PhaseID() {
        this(Manager.get(PhasestackCollector.class).stackStateID);
    }

    private PhaseID(int id) {
        this.id = Manager.get(PhasestackCollector.class).stackStateID;
    }
}
