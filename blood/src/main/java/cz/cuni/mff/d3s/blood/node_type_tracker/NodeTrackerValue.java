package cz.cuni.mff.d3s.blood.node_type_tracker;

import java.util.concurrent.atomic.AtomicLong;

//TODO consider merging it with DependencyValue.
public class NodeTrackerValue {
    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong totalCount = new AtomicLong(0);
    private final AtomicLong iterations = new AtomicLong(0);

    public void incrementNumberOfSeenNodes(long nodesSeen) {
        count.addAndGet(nodesSeen);
    }

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
