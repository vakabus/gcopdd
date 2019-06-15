package cz.cuni.mff.d3s.blood.tools.phasestack;

import cz.cuni.mff.d3s.blood.report.Manager;

public class PhaseID {

    public static final PhaseID NO_PHASE = new PhaseID(-2);
    private final int id;

    public static PhaseID getCurrent() {
        return new PhaseID(Manager.get(PhaseStackCollector.class).getStackStateID());
    }

    private PhaseID(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return Integer.toString(id);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PhaseID phaseID = (PhaseID) o;
        return id == phaseID.id;
    }

    public int getId() {
        return id;
    }
}
