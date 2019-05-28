package cz.cuni.mff.d3s.blood.report;

import java.nio.charset.Charset;

public interface TextDump extends Dump {

    @Override
    default byte[] getData() {
        return getText().getBytes(Charset.forName("utf8"));
    }

    String getText();
}
