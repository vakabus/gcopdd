package cz.cuni.mff.d3s.blood.instrumentations;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BodyMarker;
import cz.cuni.mff.d3s.blood.node_type_list.NodeTypeList;

public class NodeTypeListInject {

    @After(marker = BodyMarker.class, scope = "void Node.init(*)")
    public static void afterNodeInit(DynamicContext context) {
        NodeTypeList.getInstance().onNodeInstantiation(context.getThis().getClass());
    }
}
