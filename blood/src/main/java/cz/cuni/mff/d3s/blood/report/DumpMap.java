package cz.cuni.mff.d3s.blood.report;

import cz.cuni.mff.d3s.blood.utils.Miscellaneous;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

public final class DumpMap {

    private final HashMap<Class<? extends Dump>, Dump> map = new HashMap<>();

    @SuppressWarnings("unchecked")
    public final <T extends Dump> T get(Class<T> clazz) {
        return (T) map.computeIfAbsent(clazz, Dump::instantiate);
    }
    
    public final void dump(File reportDir, Manager.DumpConfig dumpConfig, long compilationIndex) {
        String hash = Miscellaneous.shortTextHash(dumpConfig.getCompilationUnitInfo(compilationIndex));

        try {
            NtarOutputStream dumpFile = DumpHelpers.createDumpFile(reportDir, hash);

            dumpCompilationRequestId(dumpFile, dumpConfig.getCompilationUnitInfo(compilationIndex));
            dumpTimingInformation(dumpFile, dumpConfig.getCompilationStart(), dumpConfig.getCompilationDuration());

            for (Dump dump : map.values()) {
                String name = dump.getName();
                byte[] data = dump.getData();
                dumpFile.set(name, data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dumpCompilationRequestId(NtarOutputStream dumpFile, String compilationRequestId) throws IOException {
        dumpFile.set("request", compilationRequestId.getBytes(StandardCharsets.UTF_8));
    }

    private static void dumpTimingInformation(NtarOutputStream dumpFile, Instant compilationStart, Duration compilationDuration) throws IOException {
        dumpFile.set("timing", (compilationStart.toString() + "\n" + compilationDuration.toNanos()/1000 + "\n").getBytes(StandardCharsets.UTF_8));
    }
}
