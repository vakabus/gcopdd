package cz.cuni.mff.d3s.blood.method_local;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CompilationEventLocal<T> {
    private static final ThreadLocal<List<WeakReference<CompilationEventLocal>>> allInstancesInThisThread
            = ThreadLocal.withInitial(() -> new LinkedList<>());

    final Consumer<T> onCompilationEventEnd;
    final Supplier<T> createDefault;
    final private ThreadLocal<T> data;


    public CompilationEventLocal(Supplier<T> createDefault, Consumer<T> onCompilationEventEnd) {
        this.onCompilationEventEnd = onCompilationEventEnd;
        this.createDefault = createDefault;
        this.data = ThreadLocal.withInitial(createDefault);

        // register itself for notifications about compilation event changes
        allInstancesInThisThread.get().add(new WeakReference<>(this));
    }

    public static void markNewCompilation() {
        var iterator = allInstancesInThisThread.get().iterator();
        while (iterator.hasNext()) {
            var weakInstance = iterator.next();

            if (weakInstance.get() == null) {
                // the instance has been garbage collected, so we can get rid of it
                iterator.remove();
            } else {
                var instance = weakInstance.get();

                // dispose the last object and create new one
                instance.onCompilationEventEnd.accept(instance.data.get());
                instance.data.set(instance.createDefault.get());
            }
        }
    }


    public T get() {
        return data.get();
    }
}
