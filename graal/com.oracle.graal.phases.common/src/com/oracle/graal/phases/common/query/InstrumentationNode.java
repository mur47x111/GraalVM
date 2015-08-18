package com.oracle.graal.phases.common.query;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;

@NodeInfo
public class InstrumentationNode extends AbstractStateSplit implements Virtualizable, DeoptimizingNode.DeoptAfter {

    public static final NodeClass<InstrumentationNode> TYPE = NodeClass.create(InstrumentationNode.class);

    @OptionalInput(value = InputType.Association) protected ValueNode target;
    @OptionalInput protected NodeInputList<ValueNode> weakDependencies;

    protected StructuredGraph icg;
    protected final int offset;
    protected final int type;

    public InstrumentationNode(FixedNode target, StructuredGraph icg, int offset, int type) {
        super(TYPE, StampFactory.forVoid());

        this.target = target;
        this.icg = icg;
        this.offset = offset;
        this.type = type;

        this.weakDependencies = new NodeInputList<>(this);
    }

    public boolean addInput(Node node) {
        return weakDependencies.add(node);
    }

    public ValueNode target() {
        return target;
    }

    public StructuredGraph icg() {
        return icg;
    }

    public int offset() {
        return offset;
    }

    public int type() {
        return type;
    }

    public NodeInputList<ValueNode> getWeakDependencies() {
        return weakDependencies;
    }

    public void virtualize(VirtualizerTool tool) {
        if (target instanceof MonitorEnterNode) {
            tool.replaceFirstInput(target, ((MonitorEnterNode) target).getMonitorId());
        } else {
            tool.setInvisible();
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
        for (ValueNode input : weakDependencies) {
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
    }

    public boolean canDeoptimize() {
        return true;
    }

}
