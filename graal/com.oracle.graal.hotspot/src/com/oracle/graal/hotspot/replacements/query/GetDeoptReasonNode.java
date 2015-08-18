package com.oracle.graal.hotspot.replacements.query;

import jdk.internal.jvmci.meta.*;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public final class GetDeoptReasonNode extends FixedWithNextNode implements CompilerDecisionQuery {

    public static final NodeClass<GetDeoptReasonNode> TYPE = NodeClass.create(GetDeoptReasonNode.class);

    public GetDeoptReasonNode() {
        super(TYPE, StampFactory.forKind(Kind.Boolean));
    }

    @Override
    public void onInlineICG(InstrumentationNode instrumentation, FixedNode position) {
        if (position instanceof DeoptimizeNode) {
            graph().replaceFixedWithFloating(this, ConstantNode.forInt(((DeoptimizeNode) position).action().ordinal(), graph()));
        } else {
            graph().replaceFixedWithFloating(this, ConstantNode.forInt(-1, graph()));
        }
    }

    @NodeIntrinsic
    public static native int instantiate();

}