package cz.cuni.mff.d3s.blood.phase_order_tracker;

public class PhaseOrder {
    private static ThreadLocal<Integer> phaseNumber = ThreadLocal.withInitial(() -> 0);

    public static int getCurrentPhaseNumber() {
        return phaseNumber.get();
    }

    public static void resetCounter() {
        phaseNumber.set(0);
    }

    public static void incrementCounter() {
        phaseNumber.set(phaseNumber.get() + 1);
    }
}
