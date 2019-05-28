package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BodyMarker;
import cz.cuni.mff.d3s.blood.report.Manager;
import cz.cuni.mff.d3s.blood.tools.nodelist.NodeListCollector;

/**
 * Creates a list of node classes.
 */
public class NodeListInject {

    @After(marker = BodyMarker.class, scope = "void Node.init(*)")
    public static void afterNodeInit(DynamicContext context) {
        Manager.get(NodeListCollector.class).onNodeInstantiation(context.getThis().getClass());
    }
}
