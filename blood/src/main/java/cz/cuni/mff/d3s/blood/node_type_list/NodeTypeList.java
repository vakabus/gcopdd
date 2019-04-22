package cz.cuni.mff.d3s.blood.node_type_list;

import cz.cuni.mff.d3s.blood.report.Report;
import cz.cuni.mff.d3s.blood.report.dump.ManualDump;
import org.graalvm.compiler.graph.Node;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class NodeTypeList {
    private static NodeTypeList instance = null;
    private Set<Class> nodeClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private ReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    public NodeTypeList() {
        // register dump
        Report.getInstance().registerDump(
                new ManualDump(
                        "nodelist",
                        () -> nodeClasses.stream().map(Class::getName).collect(Collectors.joining("\n")).getBytes(Charset.forName("utf8"))
                )
        );
    }

    public static NodeTypeList getInstance() {
        if (instance == null)
            instance = new NodeTypeList();

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
