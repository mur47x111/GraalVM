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

    private static void inlineICG(StructuredGraph graph, StructuredGraph icg, InstrumentationNode instrumentation, FixedNode position) {
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

        final AbstractBeginNode prevBegin = AbstractBeginNode.prevBegin(position);
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
        position.replaceAtPredecessor(firstCFGNodeDuplicate);

        if (!returnNodes.isEmpty()) {
            if (returnNodes.size() == 1) {
                ReturnNode returnNode = (ReturnNode) duplicates.get(returnNodes.get(0));
                returnNode.replaceAndDelete(position);
            } else {
                ArrayList<ReturnNode> returnDuplicates = new ArrayList<>(returnNodes.size());
                for (ReturnNode returnNode : returnNodes) {
                    returnDuplicates.add((ReturnNode) duplicates.get(returnNode));
                }
                AbstractMergeNode merge = graph.add(new MergeNode());

                for (ReturnNode returnNode : returnDuplicates) {
                    EndNode endNode = graph.add(new EndNode());
                    merge.addForwardEnd(endNode);
                    returnNode.replaceAndDelete(endNode);
                }

                merge.setNext(position);
            }
        }

        for (Node icgNode : duplicates.keySet()) {
            if (icgNode instanceof StateSplit) {
                ((StateSplit) duplicates.get(icgNode)).setStateAfter(instrumentation.stateAfter());
            }
            if (icgNode instanceof DeoptimizingNode) {
                DeoptimizingNode deoptDup = (DeoptimizingNode) duplicates.get(icgNode);
                if (deoptDup.canDeoptimize()) {
                    if (deoptDup instanceof DeoptimizingNode.DeoptDuring) {
                        ((DeoptimizingNode.DeoptDuring) deoptDup).computeStateDuring(instrumentation.stateAfter());
                    } else if (deoptDup instanceof DeoptimizingNode.DeoptAfter) {
                        ((DeoptimizingNode.DeoptAfter) deoptDup).setStateAfter(instrumentation.stateAfter());
                    }
                }
            }
            if (icgNode instanceof CompilerDecisionQueryNode) {
                ((CompilerDecisionQueryNode) duplicates.get(icgNode)).onInlineICG(instrumentation, position);
            }
        }
    }

    @Override
    protected void run(StructuredGraph graph, LowTierContext context) {
        Set<StructuredGraph> icgs = new HashSet<>();
        for (InstrumentationNode instrumentation : graph.getNodes().filter(InstrumentationNode.class)) {
            icgs.add(instrumentation.icg());
            instrumentation.stateAfter().setOuterFrameState(null);
        }

        for (StructuredGraph icg : icgs) {
            new GuardLoweringPhase().apply(icg, null, false);
            new LoweringPhase(new CanonicalizerPhase(), LoweringTool.StandardLoweringStage.LOW_TIER).apply(icg, context, false);

            for (Node node : icg.getNodes()) {
                if (node instanceof StateSplit) {
                    StateSplit stateSplit = (StateSplit) node;
                    FrameState frameState = stateSplit.stateAfter();
                    if (frameState != null) {
                        stateSplit.setStateAfter(null);
                    }
                }
            }

            new FloatingReadPhase(true, true).apply(icg, false);

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

            switch (instrumentation.type()) {
                case 1:
                    for (AbstractDeoptimizeNode deopt : graph.getNodes().filter(AbstractDeoptimizeNode.class)) {
                        inlineICG(graph, icg, instrumentation, deopt);
                    }
                    break;
                default:
                    inlineICG(graph, icg, instrumentation, instrumentation);
                    break;
            }

            GraphUtil.unlinkFixedNode(instrumentation);
            instrumentation.clearInputs();
            GraphUtil.killCFG(instrumentation);
        }

        for (CompilerDecisionQueryNode query : graph.getNodes().filter(CompilerDecisionQueryNode.class)) {
            query.replaceWithDefault();
        }

        new CanonicalizerPhase().apply(graph, context, false);
    }
}
