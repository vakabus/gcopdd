package cz.cuni.mff.d3s.blood.node_type_list;

import cz.cuni.mff.d3s.blood.report.Report;
import cz.cuni.mff.d3s.blood.report.dump.ManualTextDump;
import cz.cuni.mff.d3s.blood.utils.ConcurrentOrderedSet;
import org.graalvm.compiler.graph.Node;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class NodeTypeList {
    private static NodeTypeList instance = null;
    private final ConcurrentOrderedSet<Class> nodeClasses = new ConcurrentOrderedSet<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    public NodeTypeList() {
        Report.getInstance().registerDump(
                new ManualTextDump(
                        "nodelist",
                        () -> nodeClasses.stream().map(Class::getName).collect(Collectors.joining("\n"))
                )
        );
    }

    public static NodeTypeList getInstance() {
        if (instance == null) {
            synchronized (NodeTypeList.class) {
                if (instance == null) {
                    instance = new NodeTypeList();
                }
            }
        }

        return instance;
    }

    public void onNodeInstantiation(Class nodeClass) {
        if (!rwLock.readLock().tryLock()) {
            return;
        }

        if (!Node.class.isAssignableFrom(nodeClass)) {
            throw new UnsupportedOperationException("Classes passed into this method should be only ancestors of class Node");
        }

        nodeClasses.add(nodeClass);

        rwLock.readLock().unlock();
    }
}
