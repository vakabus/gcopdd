package cz.cuni.mff.d3s.blood.node_type_tracker;

import cz.cuni.mff.d3s.blood.method_local.CompilationEventLocal;
import cz.cuni.mff.d3s.blood.report.Report;
import cz.cuni.mff.d3s.blood.report.dump.ManualTextDump;
import cz.cuni.mff.d3s.blood.utils.ConcurrentMatrix;
import cz.cuni.mff.d3s.blood.utils.ConcurrentOrderedSet;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class NodeTypeTrackerCollector {
    private static NodeTypeTrackerCollector instance = null;

    private CompilationEventLocal<NodeTypeMatrix> matrix = new CompilationEventLocal<>(() -> new NodeTypeMatrix(), nodeTypeMatrix -> Report.getInstance().dumpNow(new ManualTextDump("nodetypematrix", nodeTypeMatrix::dump)));

    private NodeTypeTrackerCollector() {
    }

    public static NodeTypeTrackerCollector getInstance() {
        if (instance == null) {
            synchronized (NodeTypeTrackerCollector.class) {
                if (instance == null) {
                    instance = new NodeTypeTrackerCollector();
                }
            }
        }
        return instance;
    }


    /**
     * This function is called by the instrumentation before every optimization
     * phase run. More specifically, before calling
     * {@link org.graalvm.compiler.phases.BasePhase#apply(StructuredGraph, Object)}
     *
     * @param graph       Graph entering the optimization phase
     * @param sourceClass Class of the optimization phase running
     */
    public final void prePhase(StructuredGraph graph, Class<?> sourceClass) {
        matrix.get().updatePre(graph, sourceClass);
    }

    /**
     * This function is called by the instrumentation after every optimization
     * phase run. More specifically, after calling
     * {@link org.graalvm.compiler.phases.BasePhase#apply(StructuredGraph, Object)}
     *
     * @param graph       Graph representing IL after being processed by the
     *                    optimization phase
     * @param sourceClass Class of the running optimization phase
     */
    public final void postPhase(StructuredGraph graph, Class<?> sourceClass) {
        matrix.get().updatePost(graph, sourceClass);
    }


    static final class NodeTypeMatrix {
        // the default of 16 doesn't fit even the most trivial programs
        private static final int HASHMAP_INIT_CAPACITY = 64;

        private final ReadWriteLock writers = new ReentrantReadWriteLock(true);

        private ConcurrentMatrix<Class, Class, NodeTrackerValue> preMatrix = new ConcurrentMatrix<>(HASHMAP_INIT_CAPACITY, NodeTrackerValue.ZERO);
        private ConcurrentMatrix<Class, Class, NodeTrackerValue> postMatrix = new ConcurrentMatrix<>(HASHMAP_INIT_CAPACITY, NodeTrackerValue.ZERO);

        private ConcurrentOrderedSet<Class> nodeClasses = new ConcurrentOrderedSet<>();
        private ConcurrentOrderedSet<Class> phaseClasses = new ConcurrentOrderedSet<>();

        final void updatePre(StructuredGraph graph, Class<?> phaseClass) {
            update(graph, phaseClass, preMatrix);
        }

        final void updatePost(StructuredGraph graph, Class<?> phaseClass) {
            update(graph, phaseClass, postMatrix);
        }

        private void update(StructuredGraph graph, Class phaseClass, ConcurrentMatrix<Class, Class, NodeTrackerValue> matrix) {
            Lock readLock = writers.readLock();
            if (!readLock.tryLock()) {
                // Dump is already in progress, don't change the matrix any more.
                return;
            }

            try {
                phaseClasses.add(phaseClass);

                var row = matrix.getOrCreateRow(phaseClass);
                this.updateRow(graph, row);
            } finally {
                readLock.unlock();
            }
        }

        private void updateRow(StructuredGraph graph, ConcurrentMatrix<Class, Class, NodeTrackerValue>.Row row) {
            HashMap<Class, Long> nodeCount = new HashMap<>();
            for (Node n : graph.getNodes()) {
                Long count = nodeCount.getOrDefault(n.getClass(), 0l);
                count++;
                nodeCount.put(n.getClass(), count);
            }

            for (Map.Entry<Class, Long> classLongEntry : nodeCount.entrySet()) {
                Class nodeClass = classLongEntry.getKey();

                nodeClasses.add(nodeClass);

                row.getOrCreate(nodeClass)
                        .incrementNumberOfSeenNodes(classLongEntry.getValue());
            }

            final long totalCount = graph.getNodeCount();
            row.valuesStream().forEach(nodeTrackerValue -> nodeTrackerValue.incrementTotalNumberOfNodesSeen(totalCount));
            row.valuesStream().forEach(NodeTrackerValue::incrementPhaseCounter);
        }

        public final String dump() {
            // Block, so that nobody can write to the matrix.
            Lock writeLock = writers.writeLock();
            writeLock.lock();
            try {
                String nodeClassesStr = nodeClasses.stream()
                        .map(Class::getName)
                        .collect(Collectors.joining("\n"));

                String phaseClassesStr = phaseClasses.stream()
                        .map(Class::getName)
                        .collect(Collectors.joining("\n"));

                String prePhaseStr = preMatrix.toString(phaseClasses::stream, nodeClasses::stream);
                String postPhaseStr = postMatrix.toString(phaseClasses::stream, nodeClasses::stream);

                return nodeClassesStr + "\n\n" + phaseClassesStr + "\n\n" + prePhaseStr + "\n\n" + postPhaseStr;
            } finally {
                writeLock.unlock();
            }
        }
    }
}
