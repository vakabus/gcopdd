package cz.cuni.mff.d3s.blood.recompilation_tracker;

import java.time.Instant;

public class CompilationEvent {

    public final String method;
    public final int recompNumber;
    public int phases = 0;
    private final Instant started;
    private Instant finished = null;

    public CompilationEvent(String method, int recompNumber) {
        this.method = method;
        this.recompNumber = recompNumber;
        this.started = Instant.now();
    }

    public void finishNow() {
        if(finished != null)
            throw new IllegalStateException("Already finished");
        finished = Instant.now();
    }

    public Instant getStarted() {
        return started;
    }

    public Instant getFinished() {
        if(finished == null)
            throw new IllegalStateException("Not yet finished");
        return finished;
    }

    @Override
    public String toString() {
        return method + " " + recompNumber + " " + phases + " " + started + " " + finished;
    }
}
