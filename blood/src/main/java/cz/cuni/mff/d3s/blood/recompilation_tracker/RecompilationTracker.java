package cz.cuni.mff.d3s.blood.recompilation_tracker;

import cz.cuni.mff.d3s.blood.report.Report;
import cz.cuni.mff.d3s.blood.report.dump.ManualTextDump;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import static cz.cuni.mff.d3s.blood.utils.Miscellaneous.getSignatureOfMethod;
import static cz.cuni.mff.d3s.blood.utils.Miscellaneous.crGetMethod;
import static cz.cuni.mff.d3s.blood.utils.Miscellaneous.hscidGetRequest;
import java.util.stream.Collectors;
import org.graalvm.compiler.hotspot.HotSpotCompilationIdentifier;
import org.graalvm.compiler.nodes.StructuredGraph;

public class RecompilationTracker {

    private static RecompilationTracker instance = null;

    private final ConcurrentLinkedDeque<CompilationEvent> finished = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<Object, CompilationEvent> inProgress = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Object, Integer> alreadyCompiled = new ConcurrentHashMap<>();

    private RecompilationTracker() {
        // register hook for dumping data
        Report.getInstance().registerDump(new ManualTextDump("recomp", ()
                -> finished.stream().map(CompilationEvent::toString).collect(Collectors.joining("\n"))
        ));
    }

    public static RecompilationTracker getInstance() {
        if (instance == null) {
            synchronized (RecompilationTracker.class) {
                if (instance == null) {
                    instance = new RecompilationTracker();
                }
            }
        }
        return instance;
    }

    public void beforeCompileMethod(Object compilationRequest) {
        Object method = crGetMethod(compilationRequest);
        int recompNumber = alreadyCompiled.compute(method, (key, oldValue)
                -> oldValue != null ? oldValue + 1 : 1
        );
        String methodSignature = getSignatureOfMethod(method);
        if (inProgress.put(compilationRequest, new CompilationEvent(methodSignature, recompNumber)) != null) {
            throw new IllegalStateException("A CompilationEvent already exists");
        }
    }

    public void afterCompileMethod(Object compilationRequest) {
        CompilationEvent event = inProgress.remove(compilationRequest);
        event.finishNow();
        finished.add(event);
    }

    public void prePhase(StructuredGraph graph) {
        try {
            // FIXME ClassCastException: not every Identifier is HotSpotCompilationIdentifier
            Object compilationRequest = hscidGetRequest((HotSpotCompilationIdentifier) graph.compilationId());
            inProgress.get(compilationRequest).phases++;
        } catch (ClassCastException e) {
        }
    }
}
