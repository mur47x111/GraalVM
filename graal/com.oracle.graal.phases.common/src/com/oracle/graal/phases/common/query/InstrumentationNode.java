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

    @OptionalInput protected NodeInputList<ValueNode> weakDependencies;
    protected StructuredGraph icg;

    public InstrumentationNode(StructuredGraph icg) {
        super(StampFactory.forVoid());

        this.weakDependencies = new NodeInputList<>(this);
        this.icg = icg;
    }

    public boolean addInput(Node node) {
        return weakDependencies.add(node);
    }

    public void inline() {
        ArrayList<Node> nodes = new ArrayList<>(icg.getNodes().count());
        final StartNode entryPointNode = icg.start();
        FixedNode firstCFGNode = entryPointNode.next();
        ReturnNode returnNode = null;

        for (Node node : icg.getNodes()) {
            if (node == entryPointNode || node == entryPointNode.stateAfter() || node instanceof ParameterNode) {
                // Do nothing.
            } else {
                nodes.add(node);
                if (node instanceof ReturnNode) {
                    returnNode = (ReturnNode) node;
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
        ((ReturnNode) duplicates.get(returnNode)).replaceAndDelete(n);
        GraphUtil.killCFG(this);
    }

    public void lower(LoweringTool tool) {
        // propagate lowering to ICG
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase(!GraalOptions.ImmutableCode.getValue());
        PhaseContext context = new PhaseContext(tool.getMetaAccess(), tool.getConstantReflection(), tool.getLowerer(), tool.getReplacements(), tool.assumptions(), tool.getStampProvider());

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
