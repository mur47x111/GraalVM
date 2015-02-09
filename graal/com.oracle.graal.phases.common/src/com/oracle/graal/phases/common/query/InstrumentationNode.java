package com.oracle.graal.phases.common.query;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;

@NodeInfo
public class InstrumentationNode extends FixedWithNextNode {

    protected StructuredGraph icg;

    public InstrumentationNode() {
        super(StampFactory.forVoid());

        icg = new StructuredGraph();
        icg.setStart(icg.add(new StartNode()));
    }

    public void addNode(Node node) {
        icg.add(node);
    }

}
