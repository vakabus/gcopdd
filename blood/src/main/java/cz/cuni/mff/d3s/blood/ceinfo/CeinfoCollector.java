package cz.cuni.mff.d3s.blood.ceinfo;

import cz.cuni.mff.d3s.blood.report.TextDump;
import static cz.cuni.mff.d3s.blood.utils.Miscellaneous.getSignatureOfMethod;
import static cz.cuni.mff.d3s.blood.utils.Miscellaneous.crGetMethod;
import java.time.Instant;

public class CeinfoCollector implements TextDump {

    private boolean initialized = false;
    private String method = null;
    private Instant started = null;
    private Instant finished = null;

    public void init(String method) {
        if(initialized)
            throw new IllegalStateException("Already initialized");
        this.initialized = true;
        this.method = method;
        this.started = Instant.now();
    }

    public void finish() {
        if(finished != null)
            throw new IllegalStateException("Already finished");
        finished = Instant.now();
    }
    
    public void beforeCompileMethod(Object compilationRequest) {
        Object methodObject = crGetMethod(compilationRequest);
        String methodSignature = getSignatureOfMethod(methodObject);
        init(methodSignature);
    }

    public void afterCompileMethod(Object compilationRequest) {
        finish();
    }

    @Override
    public String getName() {
        return "ceinfo";
    }

    @Override
    public String getText() {
        return method + " " + started + " " + finished;
    }
}
