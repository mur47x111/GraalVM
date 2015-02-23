package com.oracle.graal.phases.query;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;

@NodeInfo
public class InstrumentationNode extends FixedWithNextNode implements Virtualizable {

    @OptionalInput protected ValueNode target;
    @OptionalInput protected NodeInputList<ValueNode> weakDependencies;

    protected InsertedCodeGraph icg;

    public InstrumentationNode(FixedNode target, StructuredGraph icg) {
        super(StampFactory.forVoid());

        this.target = target;
        this.icg = new InsertedCodeGraph(icg);
        this.weakDependencies = new NodeInputList<>(this);
    }

    public boolean addInput(Node node) {
        return weakDependencies.add(node);
    }

    public ValueNode target() {
        return target;
    }

    public InsertedCodeGraph getICG() {
        return icg;
    }

    public void setTarget(ValueNode target) {
        this.target = target;
    }

    public NodeInputList<ValueNode> getWeakDependencies() {
        return weakDependencies;
    }

    public void virtualize(VirtualizerTool tool) {
        if (target != null) {
            State state = tool.getObjectState(target);

            if (state != null && state.getState() == EscapeState.Virtual) {
                target = state.getVirtualObject();
            }
        }

        for (int i = 0; i < weakDependencies.count(); i++) {
            ValueNode input = weakDependencies.get(i);
            State state = tool.getObjectState(input);

            if (state != null && state.getState() == EscapeState.Virtual) {
                weakDependencies.set(i, state.getVirtualObject());
            }
        }

        // TODO (yz) for bypassing PEA
        // a more elegant way should be creating another edge type
        tool.setDeleted();
    }

}
