package com.oracle.graal.phases.common.query;

import java.util.*;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.nodes.virtual.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.tiers.*;

public class PostEAPhase extends BasePhase<HighTierContext> {

    private static AllocatedObjectNode getAllocatedObject(StructuredGraph graph, CommitAllocationNode commit, VirtualObjectNode virtual) {
        for (AllocatedObjectNode object : graph.getNodes().filter(AllocatedObjectNode.class)) {
            if (object.getCommit() == commit && object.getVirtualObject() == virtual) {
                return object;
            }
        }
        AllocatedObjectNode object = graph.addWithoutUnique(new AllocatedObjectNode(virtual));
        object.setCommit(commit);
        return object;
    }

    private static NodeFlood getCFGAccessible(StructuredGraph graph, FixedNode start) {
        NodeFlood flood = graph.createNodeFlood();
        flood.add(start);
        for (Node current : flood) {
            if (current instanceof LoopEndNode) {
                continue;
            } else if (current instanceof AbstractEndNode) {
                flood.add(((AbstractEndNode) current).merge());
            } else {
                flood.addAll(current.successors());
            }
        }
        return flood;
    }

    @Override
    protected void run(StructuredGraph graph, HighTierContext context) {
        for (CommitAllocationNode commit : graph.getNodes().filter(CommitAllocationNode.class)) {
            List<VirtualObjectNode> virtualObjects = commit.getVirtualObjects();
            for (int index = virtualObjects.size() - 1; index >= 0; index--) {
                VirtualObjectNode virtual = virtualObjects.get(index);
                for (InstrumentationNode instrumentation : graph.getNodes().filter(InstrumentationNode.class)) {
                    if (instrumentation.target() != virtual) {
                        continue;
                    }
                    NodeFlood flood = getCFGAccessible(graph, instrumentation);
                    if (!flood.isMarked(commit)) {
                        continue;
                    }
                    InstrumentationNode clone = (InstrumentationNode) instrumentation.copyWithInputs();
                    graph.addAfterFixed(commit, clone);
                    AllocatedObjectNode object = getAllocatedObject(graph, commit, virtual);
                    while (clone.inputs().contains(virtual)) {
                        clone.replaceFirstInput(virtual, object);
                    }
                }
            }
        }
        for (InstrumentationNode instrumentation : graph.getNodes().filter(InstrumentationNode.class)) {
            if (instrumentation.target() instanceof VirtualObjectNode) {
                GraphUtil.unlinkFixedNode(instrumentation);
                instrumentation.safeDelete();
            }
        }
    }

}
