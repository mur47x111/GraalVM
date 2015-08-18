package com.oracle.graal.phases.common.query;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;

@NodeInfo
public final class InstrumentationEndNode extends AbstractStateSplit {

    public static final NodeClass<InstrumentationEndNode> TYPE = NodeClass.create(InstrumentationEndNode.class);

    public InstrumentationEndNode() {
        super(TYPE, StampFactory.forVoid());
    }

    @NodeIntrinsic
    public static native void instantiate();

}