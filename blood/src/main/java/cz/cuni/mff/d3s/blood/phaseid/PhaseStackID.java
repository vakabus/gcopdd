package cz.cuni.mff.d3s.blood.phaseid;

import cz.cuni.mff.d3s.blood.phase_stack_tracker.PhaseStackTracker;

import java.util.Objects;

public class PhaseStackID implements PhaseID {
    final Class<?> phaseClass;
    final int sequenceNumber;

    public PhaseStackID(Class<?> phaseClass) {
        this.phaseClass = phaseClass;
        this.sequenceNumber = PhaseStackTracker.getCurrentPhaseNumber();
    }

    public PhaseStackID(Class<?> phaseClass, int sequenceNumber) {
        this.phaseClass = phaseClass;
        this.sequenceNumber = sequenceNumber;
    }

    public Class<?> getPhaseClass() {
        return phaseClass;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhaseStackID phaseID = (PhaseStackID) o;
        return sequenceNumber == phaseID.sequenceNumber &&
                phaseClass.equals(phaseID.phaseClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phaseClass, sequenceNumber);
    }

    @Override
    public String toString() {
        return phaseClass.getName() + "#" + sequenceNumber;
    }
}
