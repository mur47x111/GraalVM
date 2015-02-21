package com.oracle.graal.phases.common.query;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;

@NodeInfo
public class InstrumentationNode extends FixedWithNextNode implements Virtualizable {

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

    public void virtualize(VirtualizerTool tool) {
        for (int i = 0; i < weakDependencies.count(); i++) {
            ValueNode input = weakDependencies.get(i);
            State state = tool.getObjectState(input);

            if (state != null && state.getState() == EscapeState.Virtual) {
                weakDependencies.set(i, state.getVirtualObject());
                // TODO (yz) this is for bypassing PEA
                // a more elegant way should be creating another edge type
                tool.setDeleted();
            }
        }
    }

}
