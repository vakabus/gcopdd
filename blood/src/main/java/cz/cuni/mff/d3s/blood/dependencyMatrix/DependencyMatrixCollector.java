package cz.cuni.mff.d3s.blood.dependencyMatrix;

import cz.cuni.mff.d3s.blood.utils.CheckedConsumer;
import cz.cuni.mff.d3s.blood.utils.Miscellaneous;
import cz.cuni.mff.d3s.blood.utils.Result;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.graalvm.compiler.phases.BasePhase;

public final class DependencyMatrixCollector {

    // the default of 16 doesn't fit even the most trivial programs
    private static final int HASHMAP_INIT_CAPACITY = 64;

    private static final ConcurrentHashMap<Class, ConcurrentHashMap<Class, DependencyValue>> dependencyTable = new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY);

    /**
     * getValue.apply(rowKey).apply(columnKey) returns DependencyValue. Returns
     * value from matrix. If it encounters a null on the way, returns
     * {@link DependencyValue#ZERO}.
     */
    private static final Function<Class, Function<Class, DependencyValue>> getValue = class1 -> {
        final var row = dependencyTable.get(class1);
        return (row == null)
                ? class2 -> DependencyValue.ZERO
                : class2 -> row.getOrDefault(class2, DependencyValue.ZERO);
    };

    public static void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            dump();
        }, "dump at exit"));
    }

    public static void forceTrackCreationPosition() {
        try {
            // we want to change this static final field to true:
            // org.graalvm.compiler.graph.Node.TRACK_CREATION_POSITION

            // obtain reflection of the field we want to change
            Field trackCreationPosition = Node.class.getField("TRACK_CREATION_POSITION");

            // make the field accessible to reflection
            trackCreationPosition.setAccessible(true);

            // remove the `final` modifier, so that we can change its value
            Miscellaneous.makeNonFinal(trackCreationPosition);

            // do the actual change to its value
            trackCreationPosition.setBoolean(null, true);
        } catch (Exception ex) {
            // don't use advanced features here, this is called from a static init block
            System.err.println(ex);
        }
    }

    private static Result<StackTraceElement[], String> getCreationStackTrace(Node node) {
        // node.getCreationPosition() returns an instance of class
        // NodeCreationStackTrace, which is a subclass of NodeStackTrace.
        // Unfortunately, both are package-private and we don't have access to
        // them. But we can store them in variables using the reflection API.
        // The variable is of type Object, but always contains
        // a NodeCreationStackTrace.

        Object /*NodeCreationStackTrace*/ position = node.getCreationPosition();
        if (position == null) {
            // this happens surprisingly often
            return Result.error("Node creation position is null!");
        }

        try {
            Class NodeCreationStackTrace = position.getClass();
            Class NodeStackTrace = NodeCreationStackTrace.getSuperclass();
            Field stackTraceField = NodeStackTrace.getDeclaredField("stackTrace");
            stackTraceField.setAccessible(true);
            StackTraceElement[] stackTrace = (StackTraceElement[]) stackTraceField.get(position);
            return Result.success(stackTrace);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            return Result.error("getCreationStackTrace failed:\n" + stringWriter.toString() + "\n");
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void prePhase(StructuredGraph graph, Class<?> sourceClass) {
        ClassLoader classLoader = sourceClass.getClassLoader();

        var row = dependencyTable.get(sourceClass);
        if (row == null) {
            // FIXME race condition
            row = new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY);
            dependencyTable.put(sourceClass, row);
        }
        // "local variables referenced from a lambda expression must be final or effectively final"
        final var rowf = row;

        try {
            for (var node : graph.getNodes()) {
                Result<StackTraceElement[], String> traceResult = getCreationStackTrace(node);

                if (traceResult.isOk()) {
                    Arrays.stream(traceResult.unwrap())
                            .map(StackTraceElement::getClassName)
                            .map((Result.CheckedFunction<String, Class>) classLoader::loadClass)
                            .filter(Result::isOk) // could not be loaded => probably JVM-internal class
                            .map(Result::unwrap)
                            .filter(BasePhase.class::isAssignableFrom)
                            .findFirst()
                            .ifPresentOrElse(creationClass -> {
                                //System.out.println(creationClass.getName());
                                DependencyValue value = rowf.get(creationClass);
                                if (value == null) {
                                    // FIXME race condition
                                    value = new DependencyValue();
                                    rowf.put(creationClass, value);
                                }
                                // TODO: do something that is actually useful
                                value.update(2, 3);
                            }, () -> {
                                // created in no phase - for example during construction of the graph
                            });

                    //Arrays.stream(traceResult.unwrap()).forEachOrdered(stackTraceElement -> System.out.println(stackTraceElement.getMethodName()));
                    //System.out.println();
                } else {
                    // had to comment this out, see getCreationStackTrace
                    //System.out.println(traceResult.unwrapError());
                }
            }

        } catch (Throwable e) {
            // FIXME
            // this syncblock is here to serialize output
            synchronized (DependencyMatrixCollector.class) {
                System.err.println("prePhase function failed in DependencyMatrixCollector due to:");
                e.printStackTrace();
                System.err.println();
            }
        }
    }

    public static void postPhase(StructuredGraph graph, Class<?> sourceClass) {
        // here we wanted to mark every created node as created here
        // currently, that's unnecessary, because we are using Graal's
        // own creation tracking
    }

    private static void dump() {
        // FIXME race condition
        // graal apparently doesn't stop compiling before this shutdown hook gets called

        Instant started = Instant.now();

        final HashSet<Class> keys = new HashSet<>(HASHMAP_INIT_CAPACITY);
        dependencyTable.forEachEntry(1, entry -> {
            keys.add(entry.getKey());
            entry.getValue().forEachKey(1, keys::add);
        });
        final Class[] keysOrder = keys.toArray(new Class[keys.size()]);

        try (final OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream("/tmp/gcopdd-depmat"))) {
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

            // Dependency matrix. Lines correspond to rows.
            // Items in a row are separated by spaces.
            Arrays.stream(keysOrder)
                    .map(getValue)
                    .map(getValueFromCurrentRow
                            -> Arrays.stream(keysOrder)
                            .map(getValueFromCurrentRow)
                            .map(DependencyValue::toString)
                            .collect(Collectors.joining(" ", "", "\n"))
                    )
                    .forEachOrdered((CheckedConsumer<String>) out::append);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        Instant finished = Instant.now();
        Duration duration = Duration.between(started, finished);
        System.getLogger(DependencyMatrixCollector.class.getName()).log(
                System.Logger.Level.INFO,
                "Dependency matrix dump finished in {0} ms",
                duration.toMillis()
        );
    }
}
