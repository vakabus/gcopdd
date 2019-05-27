package cz.cuni.mff.d3s.blood.phase_stack_tracker;

import cz.cuni.mff.d3s.blood.method_local.CompilationEventLocal;
import cz.cuni.mff.d3s.blood.report.Report;
import cz.cuni.mff.d3s.blood.report.dump.ManualTextDump;

import java.util.*;
import java.util.stream.Collectors;


public class PhaseStackTracker {
    private static PhaseStackTracker instance = null;
    private final CompilationEventLocal<PhaseStack> phaseStack = new CompilationEventLocal<>(PhaseStack::new, PhaseStack::dump);

    public static PhaseStackTracker getInstance() {
        if (instance != null)
            return instance;

        synchronized (PhaseStackTracker.class) {
            if (instance == null)
                instance = new PhaseStackTracker();
            return instance;
        }
    }

    public void onPhaseEntered(Class<?> phaseClass) {
        phaseStack.get().push(phaseClass);
    }

    public void onPhaseExit(Class<?> phaseClass) {
        phaseStack.get().pop(phaseClass);
    }

    private static class PhaseStack {
        private Deque<Class> stack = new ArrayDeque<>();
        private List<String> states = new LinkedList<>();

        public void push(Class<?> phaseClass) {
            stack.addLast(phaseClass);

            // update dump
            states.add(currentStateToString());
        }

        public void pop(Class<?> phaseClass) {
            var out = stack.pollLast();

            // sanity check
            if (out != phaseClass)
                throw new AssertionError("Exiting phase that was not supposed to exit right now!");

            // update dump
            states.add(currentStateToString());
        }

        public String currentStateToString() {
            return stack.stream().map(aClass -> aClass.getName()).collect(Collectors.joining(" "));
        }


        public void dump() {
            // make list immutable, so that it can't change under our hands
            states = Collections.unmodifiableList(states);

            // dump
            Report.getInstance().dumpNow(new ManualTextDump("phaseStack", () -> String.join("\n", states)));
        }
    }
}
