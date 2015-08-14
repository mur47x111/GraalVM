package com.oracle.graal.phases.common.query;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;

@NodeInfo
public final class InstrumentationBeginNode extends FixedWithNextNode {

    public static final NodeClass<InstrumentationBeginNode> TYPE = NodeClass.create(InstrumentationBeginNode.class);

    protected final int offset;

    public InstrumentationBeginNode(int offset) {
        super(TYPE, StampFactory.forVoid());
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    @NodeIntrinsic
    public static native void instantiate(@ConstantNodeParameter int offset);

}