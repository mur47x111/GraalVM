package com.oracle.graal.hotspot.replacements.query;

import jdk.internal.jvmci.meta.*;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public final class GetDeoptBCINode extends GraalQueryNode {

    public static final NodeClass<GetDeoptBCINode> TYPE = NodeClass.create(GetDeoptBCINode.class);

    public GetDeoptBCINode() {
        super(TYPE, StampFactory.forKind(Kind.Int));
    }

    @Override
    public void onInlineICG(InstrumentationNode instrumentation, FixedNode position) {
        if (position instanceof DeoptimizeNode) {
            FrameState deoptState = ((DeoptimizeNode) position).stateBefore();
            graph().replaceFixedWithFloating(this, ConstantNode.forInt(deoptState.bci, graph()));
        } else {
            graph().replaceFixedWithFloating(this, ConstantNode.forInt(-1, graph()));
        }
    }

    @NodeIntrinsic
    public static native int instantiate();

}
