package com.oracle.graal.phases.common.query;

import java.util.*;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.virtual.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.query.*;
import com.oracle.graal.phases.tiers.*;

public class ForkICGPhase extends BasePhase<HighTierContext> {

    @Override
    protected void run(StructuredGraph graph, HighTierContext context) {
        for (Node node : graph.getNodes()) {
            if (node instanceof CommitAllocationNode) {
                CommitAllocationNode alloc = (CommitAllocationNode) node;
                List<VirtualObjectNode> virtualObjects = alloc.getVirtualObjects();

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

                                if (flood.isMarked(alloc)) {
                                    InstrumentationNode clone = (InstrumentationNode) instrumentation.copyWithInputs();
                                    graph.addAfterFixed(alloc, clone);

                                    for (Node target : graph.getNodes()) {
                                        if (target instanceof AllocatedObjectNode) {
                                            AllocatedObjectNode allocated = (AllocatedObjectNode) target;

                                            if (allocated.getCommit() == alloc && allocated.getVirtualObject() == virtual) {
                                                for (Node input : clone.inputs()) {
                                                    if (input == virtual) {
                                                        clone.replaceFirstInput(virtual, allocated);
                                                    }
                                                }

                                                break;
                                            }
                                        }
                                    }
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
