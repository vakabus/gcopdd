package cz.cuni.mff.d3s.blood.dependencyMatrix;

import java.util.concurrent.atomic.AtomicLong;

public final class DependencyValue {

    public static final DependencyValue ZERO = new DependencyValue();

    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong totalCount = new AtomicLong(0);
    private final AtomicLong iterations = new AtomicLong(0);

    public double getPercent() {
        return ((double) count.get()) / ((double) totalCount.get());
    }

    public void incrementNumberOfSeenNodes(long nodesSeen) {
        count.addAndGet(nodesSeen);
    }

    // FIXME redundant information, can be moved ouside (potential performance issue)
    public void incrementTotalNumberOfNodesSeen(long nodesTotal) {
        totalCount.addAndGet(nodesTotal);
    }

    public void incrementPhaseCounter() {
        iterations.incrementAndGet();
    }

    @Override
    public String toString() {
        return count.get() + ":" + totalCount.get() + ":" + iterations.get();
    }
}
