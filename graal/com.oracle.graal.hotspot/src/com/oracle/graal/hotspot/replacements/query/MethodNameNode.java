package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public final class MethodNameNode extends ICGMacroNode implements CompilerDecisionQuery {

    public static final NodeClass<MethodNameNode> TYPE = NodeClass.create(MethodNameNode.class);

    public MethodNameNode(Invoke invoke) {
        super(TYPE, invoke);
    }

    public ConstantNode resolve() {
        String methodName = CompilerDecisionUtil.getMethodFullName(graph().method());
        return CompilerDecisionUtil.createStringConstant(graph(), methodName);
    }

}
