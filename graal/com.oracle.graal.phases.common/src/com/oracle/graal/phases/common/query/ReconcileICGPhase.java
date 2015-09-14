package com.oracle.graal.phases.common.query;

import java.util.*;

import jdk.internal.jvmci.common.*;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.nodes.virtual.*;
import com.oracle.graal.phases.*;

public class ReconcileICGPhase extends Phase {

    public static enum State {
        POST_PEA,
        PRE_FSA
    }

    private final State state;

    public ReconcileICGPhase(State state) {
        this.state = state;
    }

    private static AllocatedObjectNode getAllocatedObject(CommitAllocationNode commit, VirtualObjectNode virtual) {
        for (AllocatedObjectNode object : commit.graph().getNodes().filter(AllocatedObjectNode.class)) {
            if (object.getCommit() == commit && object.getVirtualObject() == virtual) {
                return object;
            }
        }
        AllocatedObjectNode object = commit.graph().addWithoutUnique(new AllocatedObjectNode(virtual));
        object.setCommit(commit);
        return object;
    }

    private static void duplicateICGForMaterialization(StructuredGraph graph) {
        for (CommitAllocationNode commit : graph.getNodes().filter(CommitAllocationNode.class)) {
            List<VirtualObjectNode> virtualObjects = commit.getVirtualObjects();
            // insert ICGs in reverse order for preserving the profiling ordering
            for (int index = virtualObjects.size() - 1; index >= 0; index--) {
                VirtualObjectNode virtual = virtualObjects.get(index);
                for (InstrumentationNode instrumentation : graph.getNodes().filter(InstrumentationNode.class)) {
                    if (instrumentation.target() != virtual) {
                        continue;
                    }
                    // Insert ICG only if the CommitAllocationNode is accessible from the
                    // instrumentation node
                    NodeFlood flood = graph.createNodeFlood();
                    flood.add(instrumentation);
                    for (Node current : flood) {
                        if (current instanceof LoopEndNode) {
                            continue;
                        } else if (current instanceof AbstractEndNode) {
                            flood.add(((AbstractEndNode) current).merge());
                        } else {
                            flood.addAll(current.successors());
                        }
                    }
                    if (!flood.isMarked(commit)) {
                        continue;
                    }
                    InstrumentationNode clone = (InstrumentationNode) instrumentation.copyWithInputs();
                    graph.addAfterFixed(commit, clone);
                    AllocatedObjectNode object = getAllocatedObject(commit, virtual);
                    // Replacing all input edges originating from the VirtualObjectNode with the
                    // materialized object.
                    // Better way to do this?
                    while (clone.inputs().contains(virtual)) {
                        clone.replaceFirstInput(virtual, object);
                    }
                }
            }
        }
    }

    @Override
    protected void run(StructuredGraph graph) {
        switch (state) {
            case POST_PEA:
                duplicateICGForMaterialization(graph);
                // Remove ICG whose target is a non-materialized allocation
                for (InstrumentationNode instrumentation : graph.getNodes().filter(InstrumentationNode.class)) {
                    if (instrumentation.target() instanceof VirtualObjectNode) {
                        GraphUtil.unlinkFixedNode(instrumentation);
                        instrumentation.safeDelete();
                    }
                }
                break;

            case PRE_FSA:
                for (InstrumentationNode instrumentation : graph.getNodes().filter(InstrumentationNode.class)) {
                    ValueNode target = instrumentation.target();

                    if (target instanceof MonitorIdNode) {
                        MonitorIdNode id = (MonitorIdNode) target;
                        MonitorEnterNode enter = id.usages().filter(MonitorEnterNode.class).first();
                        if (enter != null) {
                            instrumentation.replaceFirstInput(id, enter);
                        }
                    } else if (target instanceof FixedValueAnchorNode) {
                        ValueNode value = GraphUtil.unproxify(target);
                        instrumentation.replaceFirstInput(target, value);
                    }
                }
                break;

            default:
                JVMCIError.shouldNotReachHere("unsupported ICG redirection");
        }
    }

}