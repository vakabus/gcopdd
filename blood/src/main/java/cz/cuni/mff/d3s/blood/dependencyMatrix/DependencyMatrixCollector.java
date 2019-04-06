package cz.cuni.mff.d3s.blood.dependencyMatrix;

import cz.cuni.mff.d3s.blood.utils.CheckedConsumer;
import cz.cuni.mff.d3s.blood.utils.Result;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.StructuredGraph;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.graalvm.compiler.phases.BasePhase;

public final class DependencyMatrixCollector {

    private static final Predicate<Object> NON_NULL = Predicate.isEqual(null).negate();

    // the default of 16 doesn't fit even the most trivial programs
    private static final int HASHMAP_INIT_CAPACITY = 64;

    static ConcurrentHashMap<Class, ConcurrentHashMap<Class, DependencyValue>> dependencyTable = new ConcurrentHashMap<>(HASHMAP_INIT_CAPACITY);

    public static void init() {
        // make sure, that Graal will track node creation position
        var props = System.getProperties();
        props.setProperty("debug.graal.TrackNodeCreationPosition", "true");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            dump();
        }, "dump at exit"));
    }

    private static Result<StackTraceElement[], String> getCreationStackTrace(Node node) {
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
                            .map((Result.Function<String, Class>) classLoader::loadClass)
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
        
        Date started = new Date();

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
                            -> Stream.iterate(clazz, NON_NULL, Class::getSuperclass)
                            .map(Class::getName)
                            .collect(Collectors.joining(" ", "", "\n"))
                    )
                    .forEachOrdered((CheckedConsumer<String>) out::append);

            // Empty line.
            out.append('\n');

            // Dependency matrix. Lines correspond to rows.
            // Items in a row are separated by spaces.
            Arrays.stream(keysOrder)
                    .map(dependencyTable::get)
                    .map(row
                            -> Arrays.stream(keysOrder)
                            .map(row == null ? x -> null : row::get)
                            .map(DependencyValue::toStringS)
                            .collect(Collectors.joining(" ", "", "\n"))
                    )
                    .forEachOrdered((CheckedConsumer<String>) out::append);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        Date finished = new Date();
        long timeDiff = finished.getTime() - started.getTime();
        System.getLogger(DependencyMatrixCollector.class.getName())
                .log(System.Logger.Level.INFO, "Dump finished in {0} ms", timeDiff);
    }
}
