package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public final class RootNameNode extends FixedWithNextNode implements CompilerDecisionQuery {

    public static final NodeClass<RootNameNode> TYPE = NodeClass.create(RootNameNode.class);

    public RootNameNode() {
        super(TYPE, CompilerDecisionUtil.STAMP_STRING);
    }

    @Override
    public void onInlineICG(InstrumentationNode instrumentation) {
        String methodName = CompilerDecisionUtil.getMethodFullName(instrumentation.graph().method());
        graph().replaceFixedWithFloating(this, CompilerDecisionUtil.createStringConstant(graph(), methodName));
    }

    @NodeIntrinsic
    public static native String instantiate();

}
