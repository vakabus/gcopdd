package cz.cuni.mff.d3s.blood.report;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Manager {

    /**
     * Contains dumps that are complete and are scheduled for writing out.
     */
    private static final LinkedBlockingQueue<DumpConfig> pendingDumps = new LinkedBlockingQueue<>();

    /**
     * Contains the data that are currently collected.
     */
    private static final ThreadLocal<DumpMap> dumpMap = ThreadLocal.withInitial(DumpMap::new);
    private static final ThreadLocal<Boolean> threadDumpInitialized = ThreadLocal.withInitial(() -> false);
    private static String currentlyCompiledCompilationRequest = null;

    static {
        new Thread("Dump IO") {
            @Override
            public void run() {
                try {
                    File reportDir = DumpHelpers.createReportDir();
                    long dumpIndex = 0; // protects against compilation request id collisions
                    while (true) {
                        var d = pendingDumps.take();
                        d.getDumpMap().dump(reportDir, d.getCompilationRequestId() + " #" + dumpIndex);
                        dumpIndex++;
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(DumpMap.class.getName()).log(Level.INFO, "Dump IO thread stopping due to InterruptedException.");
                }
            }
        }.start();
    }

    public static <T extends Dump> T get(Class<T> clazz) {
        return dumpMap.get().get(clazz);
    }

    public static void markNewCompilation(String compilationRequestId) {
        // dump if this thread already started compiling something
        if (threadDumpInitialized.get())
            pendingDumps.add(new DumpConfig(dumpMap.get(), currentlyCompiledCompilationRequest));
        else
            threadDumpInitialized.set(true);

        // prepare for new compilation
        dumpMap.set(new DumpMap());
        currentlyCompiledCompilationRequest = compilationRequestId;
    }


    private static final class DumpConfig {
        DumpMap dumpMap;
        String compilationRequestId;

        public DumpConfig(DumpMap dumpMap, String compilationRequestID) {
            this.dumpMap = dumpMap;
            this.compilationRequestId = compilationRequestID;
        }

        public DumpMap getDumpMap() {
            return dumpMap;
        }

        public String getCompilationRequestId() {
            return compilationRequestId;
        }
    }
}
