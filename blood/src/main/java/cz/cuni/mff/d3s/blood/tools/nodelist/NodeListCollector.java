package cz.cuni.mff.d3s.blood.tools.nodelist;

import cz.cuni.mff.d3s.blood.report.TextDump;
import org.graalvm.compiler.graph.Node;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class NodeListCollector implements TextDump {
    private final LinkedHashSet<Class> nodeClasses = new LinkedHashSet<>();

    public void onNodeInstantiation(Class nodeClass) {
        if (!Node.class.isAssignableFrom(nodeClass)) {
            throw new UnsupportedOperationException("Classes passed into this method should be only ancestors of class Node");
        }

        nodeClasses.add(nodeClass);
    }

    @Override
    public String getName() {
        return "nodelist";
    }

    @Override
    public String getText() {
        return nodeClasses.stream().map(Class::getName).collect(Collectors.joining("\n"));
    }
}
