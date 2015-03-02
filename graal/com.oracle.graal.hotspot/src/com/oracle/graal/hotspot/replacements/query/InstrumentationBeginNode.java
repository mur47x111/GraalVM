package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.common.query.ExtractICGPhase.ICGBoundaryBegin;

@NodeInfo
public final class InstrumentationBeginNode extends ICGMacroNode implements CompilerDecisionQuery, ICGBoundaryBegin {

    public static final NodeClass<InstrumentationBeginNode> TYPE = NodeClass.create(InstrumentationBeginNode.class);

    public InstrumentationBeginNode(Invoke invoke) {
        super(TYPE, invoke);
    }

    public FixedWithNextNode asFixedWithNextNode() {
        return this;
    }

}