package cz.cuni.mff.d3s.blood.instrumentations.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.marker.AbstractMarker;
import ch.usi.dag.disl.marker.AbstractDWRMarker;

/**
 * Sets the region on every method invocation instruction.
 *
 */
public class ApplyInvocationMarker extends AbstractDWRMarker {

    @Override
    public List<MarkedRegion> markWithDefaultWeavingReg(MethodNode method) {
        List<MarkedRegion> regions = new LinkedList<MarkedRegion>();

        // traverse all instructions
        InsnList instructions = method.instructions;

        for (AbstractInsnNode instruction : instructions.toArray()) {
            // check for method invocation instructions
            if (instruction instanceof MethodInsnNode && (((MethodInsnNode) instruction).name).equals("apply")) {

                // add region containing one instruction (method invocation)
                regions.add(new MarkedRegion(instruction, instruction));
            }
        }
        return regions;
    }
}