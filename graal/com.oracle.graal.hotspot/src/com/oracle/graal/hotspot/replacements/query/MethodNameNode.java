package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public class MethodNameNode extends ICGMacroNode implements CompilerDecisionQuery {

    public MethodNameNode(Invoke invoke) {
        super(invoke);
    }

    public ConstantNode resolve() {
        return graph().unique(CompilerDecisionUtil.getMethodFullName(graph().method()));
    }

}
