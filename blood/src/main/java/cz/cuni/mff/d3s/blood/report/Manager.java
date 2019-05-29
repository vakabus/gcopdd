package cz.cuni.mff.d3s.blood.report;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
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
    private static final ThreadLocal<Instant> compilationStart = ThreadLocal.withInitial(() -> null);

    static {
        new Thread("Dump IO") {
            @Override
            public void run() {
                try {
                    File reportDir = DumpHelpers.getReportDir();
                    long dumpIndex = 0; // protects against compilation request id collisions
                    while (true) {
                        var d = pendingDumps.take();
                        d.getDumpMap().dump(reportDir, d, dumpIndex);
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

    public static void markCompilationStart(String compilationRequestId) {
        dumpMap.set(new DumpMap());           // clear potentially garbage data
        compilationStart.set(Instant.now());  // save compilation start time
    }

    public static void markCompilationEnd(String compilationRequestId) {
        // calculate duration of the compilation
        Instant now = Instant.now();
        Duration compilationDuration = Duration.between(compilationStart.get(), now);

        // dump data
        pendingDumps.add(new DumpConfig(dumpMap.get(), compilationRequestId, compilationDuration, compilationStart.get()));

        // replace data with something different, so that it does not affect the currently dumped information
        dumpMap.set(new DumpMap());
    }


    public static final class DumpConfig {
        private final DumpMap dumpMap;
        private final String compilationRequestId;
        private final Duration duration;
        private final Instant compilationStart;

        public DumpConfig(DumpMap dumpMap, String compilationRequestID, Duration compilationDuration, Instant compilationStart) {
            this.dumpMap = dumpMap;
            this.compilationRequestId = compilationRequestID;
            this.duration = compilationDuration;
            this.compilationStart = compilationStart;
        }

        public DumpMap getDumpMap() {
            return dumpMap;
        }

        public String getCompilationUnitInfo(long index) {
            return compilationRequestId + " #" + index + "\n";
        }

        public Instant getCompilationStart() {
            return compilationStart;
        }

        public Duration getCompilationDuration() {
            return duration;
        }
    }
}
