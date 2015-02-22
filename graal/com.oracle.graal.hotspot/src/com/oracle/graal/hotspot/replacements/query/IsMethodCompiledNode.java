package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public class IsMethodCompiledNode extends ICGMacroNode implements CompilerDecisionQuery {

    public IsMethodCompiledNode(Invoke invoke) {
        super(invoke);
    }

    public ConstantNode resolve() {
        return ConstantNode.forBoolean(true, graph());
    }

}