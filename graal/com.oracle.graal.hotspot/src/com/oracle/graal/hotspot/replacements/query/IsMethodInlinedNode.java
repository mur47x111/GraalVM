package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public class IsMethodInlinedNode extends ICGMacroNode implements CompilerDecisionQuery {

    protected String original;

    public IsMethodInlinedNode(Invoke invoke) {
        super(invoke);
    }

    public ConstantNode resolve() {
        original = CompilerDecisionUtil.getMethodFullName(graph().method());
        return null;
    }

    public void inline(InstrumentationNode instrumentation) {
        String root = CompilerDecisionUtil.getMethodFullName(instrumentation.graph().method());
        graph().replaceFixedWithFloating(this, ConstantNode.forBoolean(!root.equals(original), graph()));
    }

}