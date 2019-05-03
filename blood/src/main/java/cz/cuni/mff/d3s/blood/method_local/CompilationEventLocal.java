package cz.cuni.mff.d3s.blood.method_local;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper for data which are local in context of a single compilation event. They are discarded every time
 * new compilation event takes place. Because compilation is also single-threaded, {@link CompilationEventLocal} data
 * are also thread local
 *
 * @param <T> Type of data being wrapped.
 */
public class CompilationEventLocal<T> implements AutoCloseable{
    private static final ThreadLocal<List<WeakReference<CompilationEventLocal>>> allInstancesInThisThread
            = ThreadLocal.withInitial(() -> new LinkedList<>());

    private static boolean FEATURE_ENABLED = false;

    final private Consumer<T> onCompilationEventEnd;
    final private Supplier<T> createDefault;

    final private ThreadLocal<T> data;
    final private T dataWhenFeatureDisabled;

    public CompilationEventLocal(Supplier<T> createDefault, Consumer<T> onCompilationEventEnd) {
        this.onCompilationEventEnd = onCompilationEventEnd;
        this.createDefault = createDefault;

        if (FEATURE_ENABLED) {
            this.data = ThreadLocal.withInitial(createDefault);
            this.dataWhenFeatureDisabled = null;
        } else {
            this.data = null;
            this.dataWhenFeatureDisabled = createDefault.get();
        }

        // register itself for notifications about compilation event changes
        allInstancesInThisThread.get().add(new WeakReference<>(this));
    }

    /**
     * This method should be called when new compilation event has occurred. It's thread-safe.
     */
    @SuppressWarnings("unchecked")
    public static void markNewCompilation() {
        var iterator = allInstancesInThisThread.get().iterator();
        while (iterator.hasNext()) {
            var instance = iterator.next().get();

            if (instance == null) {
                // the instance has been garbage collected, so we can get rid of it
                iterator.remove();
            } else {
                // dispose the last object and create new one
                instance.onCompilationEventEnd.accept(instance.data.get());
                instance.data.set(instance.createDefault.get());
            }
        }
    }

    /**
     * This method should be called after this class initialization using instrumentation.
     */
    public static void enableFeature() {
        if (allInstancesInThisThread.get().size() == 0)
            FEATURE_ENABLED = true;
        else
            throw new UnsupportedOperationException("CompilationEventLocal separation can't be activated, after it has been initialized.");
    }

    /**
     * Obtain reference to the object wrapped inside this container.
     *
     * The reference is related to the moment, when this function was called. If you keep the reference around
     * for a long time, the content of the container might change regardless of the reference you hold.
     *
     * @return object being held in this container
     */
    public T get() {
        if (FEATURE_ENABLED)
            return data.get();
        else
            return dataWhenFeatureDisabled;
    }

    /**
     * Must be called if you wan't to stop using this object now! Otherwise, the dispose method specified during construction
     * might be called a few more times, before garbage collector finally gets rid of it.
     */
    @Override
    public void close() {
        // remove reference to this object from the list of all instances, so that new compilation events won't cause
        // creation of new instances
        allInstancesInThisThread.get().removeIf(c -> c.get() == this);
    }
}
