package cz.cuni.mff.d3s.blood.report;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DumpMap {

    private final HashMap<Class<? extends Dump>, Dump> map = new HashMap<>();

    public final<T extends Dump> T get(Class<T> clazz) {
        return (T) map.computeIfAbsent(clazz, Dump::instantiate);
    }
    
    public final void dump(File reportDir, int i) {
        for (Dump dump : map.values()) {
            String name = dump.getName();
            byte[] data = dump.getData();
            try {
                DumpHelpers.createDumpFile(reportDir, name, i).write(data);
            } catch (IOException ex) {
                Logger.getLogger(DumpMap.class.getName()).log(Level.WARNING, name, ex);
            }
        }
    }
}
