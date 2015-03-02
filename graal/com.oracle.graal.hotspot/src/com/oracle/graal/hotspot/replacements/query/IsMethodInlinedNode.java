package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public final class IsMethodInlinedNode extends ICGMacroNode implements CompilerDecisionQuery {

    public static final NodeClass<IsMethodInlinedNode> TYPE = NodeClass.create(IsMethodInlinedNode.class);

    protected String original;

    public IsMethodInlinedNode(Invoke invoke) {
        super(TYPE, invoke);
    }

    public ConstantNode resolve() {
        original = CompilerDecisionUtil.getMethodFullName(graph().method());
        return null;
    }

    public void inline(InstrumentationNode instrumentation) {
        String root = CompilerDecisionUtil.getMethodFullName(graph().method());
        graph().replaceFixedWithFloating(this, ConstantNode.forBoolean(!root.equals(original), graph()));
    }

}