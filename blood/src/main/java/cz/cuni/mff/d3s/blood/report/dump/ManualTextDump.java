package cz.cuni.mff.d3s.blood.report.dump;

import java.nio.charset.Charset;
import java.util.function.Supplier;

public class ManualTextDump implements DumpRegistration{
    final String name;
    final Supplier<String> dumpProducer;

    public ManualTextDump(String name, Supplier<String> dataSupplier) {
        this.name = name;
        this.dumpProducer = dataSupplier;
    }

    @Override
    public byte[] dumpData() {
        return dumpProducer.get().getBytes(Charset.forName("utf8"));
    }

    @Override
    public String getName() {
        return name;
    }
}
