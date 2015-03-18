package com.oracle.graal.phases.query;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;

@NodeInfo
public class InstrumentationNode extends FixedWithNextNode implements Virtualizable {

    public static final NodeClass<InstrumentationNode> TYPE = NodeClass.create(InstrumentationNode.class);

    @OptionalInput(value = InputType.Association) protected ValueNode target;
    @OptionalInput protected NodeInputList<ValueNode> weakDependencies;

    protected InsertedCodeGraph icg;

    public InstrumentationNode(FixedNode target, StructuredGraph icg) {
        super(TYPE, StampFactory.forVoid());

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

    public NodeInputList<ValueNode> getWeakDependencies() {
        return weakDependencies;
    }

    public void virtualize(VirtualizerTool tool) {
        if (target instanceof MonitorEnterNode) {
            tool.replaceFirstInput(target, ((MonitorEnterNode) target).getMonitorId());
        } else {
            tool.setDeleted();

            if (target != null) {
                State state = tool.getObjectState(target);

                if (state != null) {
                    if (state.getState() == EscapeState.Virtual) {
                        tool.replaceFirstInput(target, state.getVirtualObject());
                    } else {
                        tool.replaceFirstInput(target, state.getMaterializedValue());
                    }
                }
            }
        }

        for (int i = 0; i < weakDependencies.count(); i++) {
            ValueNode input = weakDependencies.get(i);
            State state = tool.getObjectState(input);

            if (state != null) {
                if (state.getState() == EscapeState.Virtual) {
                    // TODO (yz) check if in order
                    tool.replaceFirstInput(input, state.getVirtualObject());
                } else {
                    tool.replaceFirstInput(input, state.getMaterializedValue());
                }
            }
        }

        // TODO (yz) the following statement is for cheating PEA
        // a more elegant way should be creating another edge type
    }

}
