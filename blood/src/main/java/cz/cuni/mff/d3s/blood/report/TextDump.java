package cz.cuni.mff.d3s.blood.report;

public abstract class TextDump implements Dump {

    @Override
    public byte[] getData() {
        return getText().getBytes();
    }

    public abstract String getText();
}
