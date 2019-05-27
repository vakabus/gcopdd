package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BodyMarker;
import cz.cuni.mff.d3s.blood.report.Manager;
import cz.cuni.mff.d3s.blood.nodelist.NodelistCollector;

/**
 * Creates a list of node classes.
 */
public class NodelistInject {

    @After(marker = BodyMarker.class, scope = "void Node.init(*)")
    public static void afterNodeInit(DynamicContext context) {
        Manager.get(NodelistCollector.class).onNodeInstantiation(context.getThis().getClass());
    }
}
