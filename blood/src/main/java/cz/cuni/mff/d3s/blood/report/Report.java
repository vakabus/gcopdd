package cz.cuni.mff.d3s.blood.report;

import cz.cuni.mff.d3s.blood.report.dump.DumpRegistration;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public final class Report {

    private static final System.Logger LOGGER = System.getLogger(Report.class.getName());
    private static Report instance = null;
    private final List<DumpRegistration> registrations = new ArrayList<>();
    private DumperThread dumperThread = null;

    private Report() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown, "Report shutdown hook"));
    }

    public static Report getInstance() {
        if (instance == null) {
            synchronized (Report.class) {
                if (instance == null) {
                    instance = new Report();
                }
            }
        }

        return instance;
    }

    public void registerDump(DumpRegistration registration) {
        registrations.add(registration);
    }

    public synchronized void dumpNow(DumpRegistration registration) {
        if (dumperThread == null) {
            dumperThread = new DumperThread();
            dumperThread.start();
        }

        dumperThread.dump(registration);
    }

    private void onShutdown() {
        var startTime = Instant.now();
        File reportDir = DumpHelpers.createReportDir();

        for (var dr : registrations) {
            dumpData(reportDir, dr);
        }

        Duration duration = Duration.between(startTime, Instant.now());
        LOGGER.log(System.Logger.Level.INFO, "Dump finished in {0} ms", duration.toMillis());
    }


    private void dumpData(DumpRegistration dr, boolean silent) {
        dumpData(DumpHelpers.createReportDir(), dr, silent);
    }

    private void dumpData(DumpRegistration dr) {
        dumpData(DumpHelpers.createReportDir(), dr, false);
    }

    private void dumpData(File reportDir, DumpRegistration dr) {
        dumpData(reportDir, dr, false);
    }

    private void dumpData(File reportDir, DumpRegistration dr, boolean silent) {
        if (!silent) LOGGER.log(System.Logger.Level.INFO, "Dumping {0}", dr.getName());

        var data = dr.dumpData();
        try (var fos = DumpHelpers.createDumpFile(reportDir, dr.getName())) {
            fos.write(data);
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Failed to dump report " + dr.getName(), e);
        }
    }


    private class DumperThread extends Thread {

        private final LinkedBlockingQueue<DumpRegistration> dumpQueue = new LinkedBlockingQueue<>();

        public DumperThread() {
            super("Dumping thread");
            this.setDaemon(true);
        }

        public void dump(DumpRegistration reg) {
            try {
                dumpQueue.put(reg);
            } catch (InterruptedException e) {
                throw new AssertionError("This should not happen, because queue capacity is unlimited.");
            }
        }

        @Override
        public void run() {
            while (true) {

                // FIXME JVM might exit while still dumping

                try {
                    var registration = dumpQueue.take();

                    dumpData(registration, true);
                } catch (InterruptedException e) {
                    // ignore
                }

            }
        }
    }
}
