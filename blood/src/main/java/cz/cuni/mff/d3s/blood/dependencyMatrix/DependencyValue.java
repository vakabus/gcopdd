package cz.cuni.mff.d3s.blood.dependencyMatrix;

public final class DependencyValue {
    private long count = 0;
    private long totalCount = 0;
    private long iterations = 0;

    public double getPercent() {
        return ((double) count) / ((double) totalCount);
    }

    public void update(long nodesSeen, long nodesTotal) {
        iterations++;
        count += nodesSeen;
        totalCount += nodesTotal;
    }
    
    @Override
    public String toString() {
        return count+":"+totalCount+":"+iterations;
    }
    
    public static String toStringS(DependencyValue dv) {
        return dv == null ? "0:0:0" : dv.toString();
    }
}
