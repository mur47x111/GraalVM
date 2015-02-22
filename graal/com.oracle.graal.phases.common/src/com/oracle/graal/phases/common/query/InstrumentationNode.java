package com.oracle.graal.phases.common.query;

import java.util.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.common.*;
import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.Graph.DuplicationReplacement;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.nodes.virtual.*;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.tiers.*;

@NodeInfo
public class InstrumentationNode extends FixedWithNextNode implements Lowerable, Virtualizable {

    @OptionalInput protected FixedNode target;
    @OptionalInput protected NodeInputList<ValueNode> weakDependencies;

    protected StructuredGraph icg;

    public InstrumentationNode(FixedNode target, StructuredGraph icg) {
        super(StampFactory.forVoid());

        this.target = target;
        this.icg = icg;
        this.weakDependencies = new NodeInputList<>(this);
    }

    public boolean addInput(Node node) {
        return weakDependencies.add(node);
    }

    public StructuredGraph getICG() {
        return icg;
    }

    public void resolveQueries() {
        for (Node node : icg.getNodes()) {
            if (node instanceof CompilerDecisionQuery) {
                ((CompilerDecisionQuery) node).inline(this);
            }
        }
    }

    public void inline() {
        resolveQueries();

        ArrayList<Node> nodes = new ArrayList<>(icg.getNodes().count());
        final StartNode entryPointNode = icg.start();
        FixedNode firstCFGNode = entryPointNode.next();
        ArrayList<ReturnNode> returnNodes = new ArrayList<>(4);

        for (Node node : icg.getNodes()) {
            if (node == entryPointNode || node == entryPointNode.stateAfter() || node instanceof ParameterNode) {
                // Do nothing.
            } else {
                nodes.add(node);
                if (node instanceof ReturnNode) {
                    returnNodes.add((ReturnNode) node);
                }
            }
        }

        final AbstractBeginNode prevBegin = AbstractBeginNode.prevBegin(this);
        DuplicationReplacement localReplacement = new DuplicationReplacement() {

            public Node replacement(Node node) {
                if (node instanceof ParameterNode) {
                    ValueNode value = weakDependencies.get(((ParameterNode) node).index());

                    if (value instanceof VirtualObjectNode) {
                        return graph().unique(new ConstantNode(JavaConstant.NULL_POINTER, value.stamp()));
                    } else {
                        return value;
                    }
                } else if (node == entryPointNode) {
                    return prevBegin;
                }
                return node;
            }
        };

        Map<Node, Node> duplicates = graph().addDuplicates(nodes, icg, icg.getNodeCount(), localReplacement);
        FixedNode firstCFGNodeDuplicate = (FixedNode) duplicates.get(firstCFGNode);
        replaceAtPredecessor(firstCFGNodeDuplicate);

        FixedNode n = next();
        setNext(null);

        if (!returnNodes.isEmpty()) {
            if (returnNodes.size() == 1) {
                ReturnNode returnNode = (ReturnNode) duplicates.get(returnNodes.get(0));
                returnNode.replaceAndDelete(n);
            } else {
                ArrayList<ReturnNode> returnDuplicates = new ArrayList<>(returnNodes.size());
                for (ReturnNode returnNode : returnNodes) {
                    returnDuplicates.add((ReturnNode) duplicates.get(returnNode));
                }
                AbstractMergeNode merge = graph().add(new MergeNode());

                for (ReturnNode returnNode : returnDuplicates) {
                    EndNode endNode = graph().add(new EndNode());
                    merge.addForwardEnd(endNode);
                    returnNode.replaceAndDelete(endNode);
                }

                merge.setNext(n);
            }
        }

// ((ReturnNode) duplicates.get(returnNode)).replaceAndDelete(n);
        GraphUtil.killCFG(this);
    }

    public void lower(LoweringTool tool) {
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase(!GraalOptions.ImmutableCode.getValue());
        PhaseContext context = new PhaseContext(tool.getMetaAccess(), tool.getConstantReflection(), tool.getLowerer(), tool.getReplacements(), tool.assumptions(), tool.getStampProvider());

        // propagate lowering to ICG
        canonicalizer.apply(icg, context);
        new LoweringPhase(canonicalizer, tool.getLoweringStage()).apply(icg, context);
        new DeadCodeEliminationPhase().apply(icg);

        Debug.dump(icg, "After lowering ICG at " + tool.getLoweringStage());
    }

    public void virtualize(VirtualizerTool tool) {
        for (int i = 0; i < weakDependencies.count(); i++) {
            ValueNode input = weakDependencies.get(i);
            State state = tool.getObjectState(input);

            if (state != null && state.getState() == EscapeState.Virtual) {
                weakDependencies.set(i, state.getVirtualObject());
                // TODO (yz) for bypassing PEA
                // a more elegant way should be creating another edge type
                tool.setDeleted();
            }
        }
    }

}
