package cz.cuni.mff.d3s.blood.report;

import cz.cuni.mff.d3s.blood.report.dump.DumpRegistration;
import java.io.File;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Report {

    private static Report instance = null;
    private static final System.Logger LOGGER = System.getLogger(Report.class.getName());
    private final List<DumpRegistration> registrations = new ArrayList<>();

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

    private void onShutdown() {
        var startTime = Instant.now();
        File reportDir = DumpHelpers.createReportDir();

        for (var dr : registrations) {
            LOGGER.log(System.Logger.Level.INFO, "Dumping {0}", dr.getName());

            var data = dr.dumpData();
            try (var fos = DumpHelpers.createDumpFile(reportDir, dr.getName())) {
                fos.write(data);
            } catch (IOException e) {
                LOGGER.log(System.Logger.Level.ERROR, "Failed to dump report " + dr.getName(), e);
            }
        }

        Duration duration = Duration.between(startTime, Instant.now());
        LOGGER.log(System.Logger.Level.INFO, "Dump finished in {0} ms", duration.toMillis());
    }
}