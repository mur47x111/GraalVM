package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public final class RootNameNode extends ICGMacroNode implements CompilerDecisionQuery {

    public static final NodeClass<RootNameNode> TYPE = NodeClass.create(RootNameNode.class);

    public RootNameNode(Invoke invoke) {
        super(TYPE, invoke);
    }

    public void inline(InstrumentationNode instrumentation) {
        String methodName = CompilerDecisionUtil.getMethodFullName(graph().method());
        graph().replaceFixedWithFloating(this, CompilerDecisionUtil.createStringConstant(graph(), methodName));
    }

}
