package cz.cuni.mff.d3s.blood.report;

import cz.cuni.mff.d3s.blood.utils.Miscellaneous;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DumpMap {

    private final HashMap<Class<? extends Dump>, Dump> map = new HashMap<>();

    @SuppressWarnings("unchecked")
    public final <T extends Dump> T get(Class<T> clazz) {
        return (T) map.computeIfAbsent(clazz, Dump::instantiate);
    }
    
    public final void dump(File reportDir, Manager.DumpConfig dumpConfig, long compilationIndex) {
        String hash = Miscellaneous.shortTextHash(dumpConfig.getCompilationUnitInfo(compilationIndex));

        dumpCompilationRequestId(reportDir, dumpConfig.getCompilationUnitInfo(compilationIndex), hash);
        dumpTimingInformation(reportDir, dumpConfig.getCompilationStart(), dumpConfig.getCompilationDuration(), hash);

        for (Dump dump : map.values()) {
            String name = dump.getName();
            byte[] data = dump.getData();
            try (var fos = DumpHelpers.createDumpFile(reportDir, name, hash)) {
                fos.write(data);
            } catch (IOException ex) {
                Logger.getLogger(DumpMap.class.getName()).log(Level.WARNING, name, ex);
            }
        }
    }

    private void dumpCompilationRequestId(File reportDir, String compilationRequestId, String id) {
        try (var fos = DumpHelpers.createDumpFile(reportDir, "request", id)) {
            fos.write(compilationRequestId.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void dumpTimingInformation(File reportDir, Instant compilationStart, Duration compilationDuration, String id) {
        try (var fos = DumpHelpers.createDumpFile(reportDir, "timing", id)) {
            fos.write((compilationStart.toString() + "\n" + compilationDuration.toNanos()/1000 + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
