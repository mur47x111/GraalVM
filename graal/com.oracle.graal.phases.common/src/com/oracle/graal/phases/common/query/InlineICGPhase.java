package com.oracle.graal.phases.common.query;

import java.util.*;

import jdk.internal.jvmci.meta.*;

import com.oracle.graal.graph.Graph.DuplicationReplacement;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.memory.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.nodes.virtual.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.tiers.*;

public class InlineICGPhase extends BasePhase<LowTierContext> {

    @Override
    protected void run(StructuredGraph graph, LowTierContext context) {
        Set<StructuredGraph> icgs = new HashSet<>();
        for (InstrumentationNode instrumentation : graph.getNodes().filter(InstrumentationNode.class)) {
            icgs.add(instrumentation.icg());
        }

        for (StructuredGraph icg : icgs) {
            new GuardLoweringPhase().apply(icg, null);
            new FrameStateAssignmentPhase().apply(icg);
            new LoweringPhase(new CanonicalizerPhase(), LoweringTool.StandardLoweringStage.LOW_TIER).apply(icg, context);
            new FloatingReadPhase(true, true).apply(icg);

            MemoryAnchorNode anchor = icg.add(new MemoryAnchorNode());
            icg.start().replaceAtUsages(InputType.Memory, anchor);
            if (anchor.hasNoUsages()) {
                anchor.safeDelete();
            } else {
                icg.addAfterFixed(icg.start(), anchor);
            }
        }

        for (InstrumentationNode instrumentation : graph.getNodes().filter(InstrumentationNode.class)) {
            StructuredGraph icg = instrumentation.icg();

            ArrayList<Node> nodes = new ArrayList<>(icg.getNodes().count());
            final StartNode entryPointNode = icg.start();
            FixedNode firstCFGNode = entryPointNode.next();
            ArrayList<ReturnNode> returnNodes = new ArrayList<>(4);

            for (Node icgnode : icg.getNodes()) {
                if (icgnode == entryPointNode || icgnode == entryPointNode.stateAfter() || icgnode instanceof ParameterNode) {
                    // Do nothing.
                } else {
                    nodes.add(icgnode);
                    if (icgnode instanceof ReturnNode) {
                        returnNodes.add((ReturnNode) icgnode);
                    }
                }
            }

            final AbstractBeginNode prevBegin = AbstractBeginNode.prevBegin(instrumentation);
            DuplicationReplacement localReplacement = new DuplicationReplacement() {

                public Node replacement(Node replacement) {
                    if (replacement instanceof ParameterNode) {
                        ValueNode value = instrumentation.getWeakDependencies().get(((ParameterNode) replacement).index());

                        if (value == null || value.isDeleted() || value instanceof VirtualObjectNode || value.stamp().getStackKind() != Kind.Object) {
                            return graph.unique(new ConstantNode(JavaConstant.NULL_POINTER, ((ParameterNode) replacement).stamp()));
                        } else {
                            return value;
                        }
                    } else if (replacement == entryPointNode) {
                        return prevBegin;
                    }
                    return replacement;
                }
            };

            Map<Node, Node> duplicates = graph.addDuplicates(nodes, icg, icg.getNodeCount(), localReplacement);

            FixedNode firstCFGNodeDuplicate = (FixedNode) duplicates.get(firstCFGNode);
            instrumentation.replaceAtPredecessor(firstCFGNodeDuplicate);

            FixedNode n = instrumentation.next();
            instrumentation.setNext(null);

            if (!returnNodes.isEmpty()) {
                if (returnNodes.size() == 1) {
                    ReturnNode returnNode = (ReturnNode) duplicates.get(returnNodes.get(0));
                    MemoryNode lastAccess = returnNode.getMemoryMap().getLastLocationAccess(instrumentation.getLocationIdentity());
                    instrumentation.replaceAtUsages(lastAccess.asNode());
                    returnNode.replaceAndDelete(n);
                } else {
                    ArrayList<ReturnNode> returnDuplicates = new ArrayList<>(returnNodes.size());
                    for (ReturnNode returnNode : returnNodes) {
                        returnDuplicates.add((ReturnNode) duplicates.get(returnNode));
                    }
                    AbstractMergeNode merge = graph.add(new MergeNode());
                    MemoryPhiNode phi = graph.addWithoutUnique(new MemoryPhiNode(merge, instrumentation.getLocationIdentity()));

                    for (ReturnNode returnNode : returnDuplicates) {
                        EndNode endNode = graph.add(new EndNode());
                        merge.addForwardEnd(endNode);
                        phi.addInput(returnNode.getMemoryMap().getLastLocationAccess(instrumentation.getLocationIdentity()).asNode());
                        returnNode.replaceAndDelete(endNode);
                    }

                    instrumentation.replaceAtUsages(phi);
                    merge.setNext(n);
                }
            }

            for (Node node : graph.getNodes()) {
                if (node instanceof CompilerDecisionQuery) {
                    ((CompilerDecisionQuery) node).onInlineICG(instrumentation);
                }
            }

            instrumentation.clearInputs();
            GraphUtil.killCFG(instrumentation);
        }
    }

}
