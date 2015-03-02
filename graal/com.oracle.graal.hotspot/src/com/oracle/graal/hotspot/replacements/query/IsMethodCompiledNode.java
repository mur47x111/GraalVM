package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public final class IsMethodCompiledNode extends ICGMacroNode implements CompilerDecisionQuery {

    public static final NodeClass<IsMethodCompiledNode> TYPE = NodeClass.create(IsMethodCompiledNode.class);

    public IsMethodCompiledNode(Invoke invoke) {
        super(TYPE, invoke);
    }

    public ConstantNode resolve() {
        return ConstantNode.forBoolean(true, graph());
    }

}