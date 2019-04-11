package cz.cuni.mff.d3s.blood.dependencyMatrix;

import cz.cuni.mff.d3s.blood.utils.Miscellaneous;
import cz.cuni.mff.d3s.blood.utils.Result;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.phases.BasePhase;

/**
 * A wrapper around the node tracking facilities built into Graal.
 */
public class DefaultNodeTracker implements NodeTracker {

    private Field stackTraceField;

    private static void forceTrackCreationPosition() {
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

    private void extractStackTraceField() {
        Arrays.stream(Node.class.getDeclaredClasses())
                .filter(clazz -> clazz.getSimpleName().equals("NodeStackTrace"))
                .findAny()
                .ifPresentOrElse(NodeStackTrace -> {
                    try {
                        stackTraceField = NodeStackTrace.getDeclaredField("stackTrace");
                        stackTraceField.setAccessible(true);
                    } catch (NoSuchFieldException | SecurityException e) {
                        e.printStackTrace();
                    }
                }, () -> System.err.println("Could not get NodeStackTrace class out of Node class"));
    }

    private Result<StackTraceElement[], String> getCreationStackTrace(Node node) {
        // node.getCreationPosition() returns an instance of a private class,
        // so we have to store it in a variable of type Object and
        // use reflection to get access to its `stackTrace` field.

        Object /*NodeCreationStackTrace*/ position = node.getCreationPosition();

        if (position == null) {
            // The information had been deleted.
            return null;
        }

        try {
            StackTraceElement[] stackTrace = (StackTraceElement[]) stackTraceField.get(position);
            return Result.success(stackTrace);
        } catch (IllegalAccessException e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            return Result.error("getCreationStackTrace failed:\n" + stringWriter.toString() + "\n");
        }
    }

    @Override
    public void onNodeClassInit() {
        forceTrackCreationPosition();
        extractStackTraceField();
    }

    @Override
    public Result<Class<?>, String> getCreationPhase(Node node) {
        ClassLoader classLoader = node.getClass().getClassLoader();

        Result<StackTraceElement[], String> traceResult = getCreationStackTrace(node);

        if (traceResult == null) {
            return Result.success(DeletedPhaseDummy.class);
        }

        if (traceResult.isError()) {
            return Result.error("getCreationPhase failed: " + traceResult.unwrapError());
        }

        return Result.success(Arrays.stream(traceResult.unwrap())
                .map(StackTraceElement::getClassName)
                .map((Result.CheckedFunction<String, Class>) classLoader::loadClass)
                .filter(Result::isOk) // could not be loaded => probably JVM-internal class
                .map(Result::unwrap)
                .filter(BasePhase.class::isAssignableFrom)
                .findFirst()
                .orElse(NoPhaseDummy.class));
    }

    @Override
    public void updateCreationPhase(Iterable<Node> nodes, Class<?> phaseClass) {
        // We don't need to mark nodes, as Graal does it for us.
    }
}
