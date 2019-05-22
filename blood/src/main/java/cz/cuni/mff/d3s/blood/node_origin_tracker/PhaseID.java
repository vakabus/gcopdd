package cz.cuni.mff.d3s.blood.node_origin_tracker;

import cz.cuni.mff.d3s.blood.phase_order_tracker.PhaseOrder;
import org.graalvm.compiler.phases.BasePhase;

import java.util.Objects;

public class PhaseID {
    final Class<?> phaseClass;
    final int sequenceNumber;

    public PhaseID(Class<?> phaseClass) {
        this.phaseClass = phaseClass;
        this.sequenceNumber = PhaseOrder.getCurrentPhaseNumber();
    }

    public PhaseID(Class<?> phaseClass, int sequenceNumber) {
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
        PhaseID phaseID = (PhaseID) o;
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
