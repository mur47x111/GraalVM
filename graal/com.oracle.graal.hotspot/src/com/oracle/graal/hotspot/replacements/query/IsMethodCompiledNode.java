package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.graph.spi.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.ExtractICGPhase.CompilerDecisionQuery;
import com.oracle.graal.replacements.nodes.*;

@NodeInfo
public class IsMethodCompiledNode extends MacroNode implements Canonicalizable, CompilerDecisionQuery {

    public IsMethodCompiledNode(Invoke invoke) {
        super(invoke);
    }

    public Node canonical(CanonicalizerTool tool) {
        return ConstantNode.forBoolean(true, graph());
    }

}