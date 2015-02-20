package com.oracle.graal.phases.common.query;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;

@NodeInfo
public class InstrumentationNode extends FixedWithNextNode implements LIRLowerable {

    @Input protected NodeInputList<ValueNode> weakDependencies;
    protected StructuredGraph icg;

    public InstrumentationNode(StructuredGraph icg) {
        super(StampFactory.forVoid());

        this.weakDependencies = new NodeInputList<>(this);
        this.icg = icg;
    }

    public boolean addInput(Node node) {
        return weakDependencies.add(node);
    }

    public void generate(NodeLIRBuilderTool generator) {
        // TODO Auto-generated method stub

    }

}
