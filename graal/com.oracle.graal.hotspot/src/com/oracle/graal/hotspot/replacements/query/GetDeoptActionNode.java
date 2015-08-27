package com.oracle.graal.hotspot.replacements.query;

import jdk.internal.jvmci.meta.*;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public final class GetDeoptActionNode extends GraalQueryNode {

    public static final NodeClass<GetDeoptActionNode> TYPE = NodeClass.create(GetDeoptActionNode.class);

    public GetDeoptActionNode() {
        super(TYPE, StampFactory.forKind(Kind.Int));
    }

    @Override
    public void onInlineICG(InstrumentationNode instrumentation, FixedNode position) {
        if (position instanceof DeoptimizeNode) {
            graph().replaceFixedWithFloating(this, ConstantNode.forInt(((DeoptimizeNode) position).reason().ordinal(), graph()));
        } else {
            graph().replaceFixedWithFloating(this, ConstantNode.forInt(-1, graph()));
        }
    }

    @NodeIntrinsic
    public static native int instantiate();

}