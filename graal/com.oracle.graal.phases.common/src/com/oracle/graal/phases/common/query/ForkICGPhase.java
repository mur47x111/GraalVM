package com.oracle.graal.phases.common.query;

import java.util.*;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.virtual.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.query.*;
import com.oracle.graal.phases.tiers.*;

public class ForkICGPhase extends BasePhase<HighTierContext> {

    private static AllocatedObjectNode getAllocatedObject(StructuredGraph graph, CommitAllocationNode alloc, VirtualObjectNode virtual) {
        for (Node target : graph.getNodes()) {
            if (target instanceof AllocatedObjectNode) {
                AllocatedObjectNode object = (AllocatedObjectNode) target;

                if (object.getCommit() == alloc && object.getVirtualObject() == virtual) {
                    return object;
                }
            }
        }
        return null;
    }

    @Override
    protected void run(StructuredGraph graph, HighTierContext context) {
        for (Node node : graph.getNodes()) {
            if (node instanceof CommitAllocationNode) {
                CommitAllocationNode commit = (CommitAllocationNode) node;
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

                                    clone.replaceFirstInput(virtual, object);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (Node node : graph.getNodes()) {
            if (node instanceof InstrumentationNode) {
                InstrumentationNode instrumentation = (InstrumentationNode) node;
                ValueNode target = instrumentation.target();

                if (target instanceof VirtualObjectNode) {
                    instrumentation.replaceFirstInput(target, null);
                }
            }
        }
    }
}
