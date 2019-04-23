package cz.cuni.mff.d3s.blood.report.dump;

import java.util.function.Supplier;

public final class ManualBinaryDump implements DumpRegistration {
    final String name;
    final Supplier<byte[]> dumpProducer;

    public ManualBinaryDump(String name, Supplier<byte[]> dataSupplier) {
        this.name = name;
        this.dumpProducer = dataSupplier;
    }

    @Override
    public byte[] dumpData() {
        return dumpProducer.get();
    }

    @Override
    public String getName() {
        return name;
    }
}
