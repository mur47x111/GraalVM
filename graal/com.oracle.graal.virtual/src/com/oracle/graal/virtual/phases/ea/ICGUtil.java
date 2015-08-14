package com.oracle.graal.virtual.phases.ea;

import java.util.*;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.nodes.virtual.*;
import com.oracle.graal.phases.query.*;

public class ICGUtil {

    private static AllocatedObjectNode getAllocatedObject(StructuredGraph graph, CommitAllocationNode alloc, VirtualObjectNode virtual) {
        for (AllocatedObjectNode object : graph.getNodes().filter(AllocatedObjectNode.class)) {
            if (object.getCommit() == alloc && object.getVirtualObject() == virtual) {
                return object;
            }
        }
        return null;
    }

    public static void redirect(StructuredGraph graph) {
        for (CommitAllocationNode commit : graph.getNodes().filter(CommitAllocationNode.class)) {
            List<VirtualObjectNode> virtualObjects = commit.getVirtualObjects();

            for (int index = virtualObjects.size() - 1; index >= 0; index--) {
                VirtualObjectNode virtual = virtualObjects.get(index);

                for (Node usage : graph.getNodes()) {
                    if (usage instanceof InstrumentationNode) {
                        InstrumentationNode instrumentation = (InstrumentationNode) usage;

                        if (instrumentation.target() == virtual) {
                            NodeFlood flood = graph.createNodeFlood();
                            flood.add(usage);

                            for (Node current : flood) {
                                if (current instanceof LoopEndNode) {
                                    continue;
                                } else if (current instanceof AbstractEndNode) {
                                    AbstractEndNode end = (AbstractEndNode) current;
                                    flood.add(end.merge());
                                } else {
                                    for (Node successor : current.successors()) {
                                        flood.add(successor);
                                    }
                                }
                            }

                            if (flood.isMarked(commit)) {
                                InstrumentationNode clone = (InstrumentationNode) instrumentation.copyWithInputs();
                                graph.addAfterFixed(commit, clone);

                                AllocatedObjectNode object = getAllocatedObject(graph, commit, virtual);

                                if (object == null) {
                                    object = graph.addWithoutUnique(new AllocatedObjectNode(virtual));
                                    object.setCommit(commit);
                                }

                                while (clone.inputs().contains(virtual)) {
                                    clone.replaceFirstInput(virtual, object);
                                }
                            }
                        }
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
