package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public final class GetRootNameNode extends GraalQueryNode {

    public static final NodeClass<GetRootNameNode> TYPE = NodeClass.create(GetRootNameNode.class);

    public GetRootNameNode() {
        super(TYPE, CompilerDecisionUtil.STAMP_STRING);
    }

    @Override
    public void onInlineICG(InstrumentationNode instrumentation, FixedNode position) {
        String methodName = CompilerDecisionUtil.getMethodFullName(graph().method());
        graph().replaceFixedWithFloating(this, CompilerDecisionUtil.createStringConstant(graph(), methodName));
    }

    @NodeIntrinsic
    public static native String instantiate();

}
