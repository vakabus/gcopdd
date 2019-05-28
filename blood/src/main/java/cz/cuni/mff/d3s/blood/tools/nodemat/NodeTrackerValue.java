package cz.cuni.mff.d3s.blood.tools.nodemat;

import cz.cuni.mff.d3s.blood.utils.matrix.MatrixValue;
import java.util.concurrent.atomic.AtomicLong;

//TODO consider merging it with DependencyValue.
public class NodeTrackerValue implements MatrixValue<NodeTrackerValue> {

    public static final NodeTrackerValue ZERO = new NodeTrackerValue();

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

    @Override
    public NodeTrackerValue copy() {
        NodeTrackerValue newObject = new NodeTrackerValue();
        newObject.count.set(count.get());
        newObject.totalCount.set(totalCount.get());
        newObject.iterations.set(iterations.get());
        return newObject;
    }
}
