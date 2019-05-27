package cz.cuni.mff.d3s.blood.report;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Manager {

    /**
     * Contains dumps that are complete and are scheduled for writing out.
     */
    private static final LinkedBlockingQueue<DumpMap> pendingDumps = new LinkedBlockingQueue<>();

    /**
     * Contains the data that are currently collected.
     */
    private static final ThreadLocal<DumpMap> dumpMap = ThreadLocal.withInitial(DumpMap::new);

    static {
        new Thread("Dump IO") {
            @Override
            public void run() {
                try {
                    File reportDir = DumpHelpers.createReportDir();
                    for (int i = 0;; i++) {
                        pendingDumps.take().dump(reportDir, i);
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

    public static void markNewCompilation() {
        pendingDumps.add(dumpMap.get());
        dumpMap.set(new DumpMap());
    }
}
