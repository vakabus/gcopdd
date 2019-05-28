package cz.cuni.mff.d3s.blood.report;

import cz.cuni.mff.d3s.blood.utils.Miscellaneous;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DumpMap {

    private final HashMap<Class<? extends Dump>, Dump> map = new HashMap<>();

    @SuppressWarnings("unchecked")
    public final <T extends Dump> T get(Class<T> clazz) {
        return (T) map.computeIfAbsent(clazz, Dump::instantiate);
    }
    
    public final void dump(File reportDir, String compilationRequestId) {
        String suffix = Miscellaneous.shortTextHash(compilationRequestId);

        dumpCompilationRequestId(reportDir, compilationRequestId, suffix);

        for (Dump dump : map.values()) {
            String name = dump.getName();
            byte[] data = dump.getData();
            try (var fos = DumpHelpers.createDumpFile(reportDir, name, suffix)) {
                fos.write(data);
            } catch (IOException ex) {
                Logger.getLogger(DumpMap.class.getName()).log(Level.WARNING, name, ex);
            }
        }
    }

    private void dumpCompilationRequestId(File reportDir, String compilationRequestId, String suffix) {
        try (var fos = DumpHelpers.createDumpFile(reportDir, "request", suffix)) {
            fos.write(compilationRequestId.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
