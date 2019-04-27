package cz.cuni.mff.d3s.blood.node_type_tracker;

import cz.cuni.mff.d3s.blood.report.Report;
import cz.cuni.mff.d3s.blood.report.dump.ManualTextDump;
import cz.cuni.mff.d3s.blood.utils.CheckedConsumer;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NodeTypeTrackerCollector {

    // the default of 16 doesn't fit even the most trivial programs
    private static final int HASHMAP_INIT_CAPACITY = 64;

    private static NodeTypeTrackerCollector instance = null;

    private final ReadWriteLock writers = new ReentrantReadWriteLock(true);

    private final ConcurrentHashMap<Class, ConcurrentHashMap<Class, NodeTrackerValue>> postPhaseTypes = new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY);
    private final ConcurrentHashMap<Class, ConcurrentHashMap<Class, NodeTrackerValue>> prePhaseTypes = new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY);

    private final Function<Class, Function<Class, NodeTrackerValue>> getPostValue = class1 -> {
        final var row = postPhaseTypes.get(class1);
        return (row == null)
                ? class2 -> NodeTrackerValue.ZERO
                : class2 -> row.getOrDefault(class2, NodeTrackerValue.ZERO);
    };

    private final Function<Class, Function<Class, NodeTrackerValue>> getPreValue = class1 -> {
        final var row = prePhaseTypes.get(class1);
        return (row == null)
                ? class2 -> NodeTrackerValue.ZERO
                : class2 -> row.getOrDefault(class2, NodeTrackerValue.ZERO);
    };

    private NodeTypeTrackerCollector() {
        Report.getInstance().registerDump(new ManualTextDump("nodetypematrix", this::dump));
    }

    public static NodeTypeTrackerCollector getInstance() {
        if (instance == null) {
            synchronized (NodeTypeTrackerCollector.class) {
                if (instance == null)
                    instance = new NodeTypeTrackerCollector();
            }
        }
        return instance;
    }

    public final void updateRow(StructuredGraph graph, ConcurrentHashMap<Class, NodeTrackerValue> phaseTable) {
        HashMap<Class, Long> nodeCount = new HashMap<>();
        for (Node n : graph.getNodes()) {
            Long count = nodeCount.getOrDefault(n.getClass(), 0l);
            count++;
            nodeCount.put(n.getClass(), count);
        }

        for (Map.Entry<Class, Long> classLongEntry : nodeCount.entrySet()) {
            Class nodeClass = classLongEntry.getKey();

            phaseTable.computeIfAbsent(nodeClass, aClass -> new NodeTrackerValue())
                    .incrementNumberOfSeenNodes(classLongEntry.getValue());
        }

        final long totalCount = graph.getNodeCount();
        phaseTable.values().stream().forEach(nodeTrackerValue -> nodeTrackerValue.incrementTotalNumberOfNodesSeen(totalCount));
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
        ConcurrentHashMap<Class, NodeTrackerValue> row = prePhaseTypes.computeIfAbsent(sourceClass, aClass -> new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY));
        updateRow(graph, row);
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
        ConcurrentHashMap<Class, NodeTrackerValue> row = postPhaseTypes.computeIfAbsent(sourceClass, aClass -> new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY));
        updateRow(graph, row);
    }

    /**
     * Method called on JVM exit dumping collected statistics.
     */
    private String dump() {
        // Block, so that nobody can write to the matrix.
        Lock writeLock = writers.writeLock();
        writeLock.lock();
        StringWriter out = new StringWriter();
        try {

            // collect all node classes
            final Class[] keysOrder = Stream.concat(prePhaseTypes.entrySet().stream(), postPhaseTypes.entrySet().stream()).flatMap(entry
                    -> Stream.concat(Stream.of(entry.getKey()), entry.getValue().keySet().stream())
            ).collect(Collectors.toSet()).toArray(i -> new Class[i]);

            // List of classes in the order that is used in the matrix below.
            // Each line contains space-separated list of the class's
            // superclasses from itself up to java.lang.Object.
            Arrays.stream(keysOrder)
                    .map(clazz
                            -> Stream.iterate(clazz, Predicate.isEqual(null).negate(), Class::getSuperclass)
                            .map(Class::getName)
                            .collect(Collectors.joining(" ", "", "\n"))
                    )
                    .forEachOrdered((CheckedConsumer<String>) out::append);

            // Empty line.
            out.append('\n');

            // prePhaseTypes matrix. Lines correspond to rows.
            // Items in a row are separated by spaces.
            Arrays.stream(keysOrder)
                    .map(getPreValue)
                    .map(getValueFromCurrentRow
                            -> Arrays.stream(keysOrder)
                            .map(getValueFromCurrentRow)
                            .map(NodeTrackerValue::toString)
                            .collect(Collectors.joining(" ", "", "\n"))
                    )
                    .forEachOrdered((CheckedConsumer<String>) out::append);

            // postPhaseTypes matrix. Lines correspond to rows.
            // Items in a row are separated by spaces.
            Arrays.stream(keysOrder)
                    .map(getPostValue)
                    .map(getValueFromCurrentRow
                            -> Arrays.stream(keysOrder)
                            .map(getValueFromCurrentRow)
                            .map(NodeTrackerValue::toString)
                            .collect(Collectors.joining(" ", "", "\n"))
                    )
                    .forEachOrdered((CheckedConsumer<String>) out::append);
        } finally {
            writeLock.unlock();
        }

        return out.toString();
    }
}
