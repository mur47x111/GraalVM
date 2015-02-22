package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.common.query.ExtractICGPhase.ICGBoundaryBegin;

@NodeInfo
public class InstrumentationBeginNode extends ICGMacroNode implements CompilerDecisionQuery, ICGBoundaryBegin {

    public InstrumentationBeginNode(Invoke invoke) {
        super(invoke);
    }

    public FixedWithNextNode asFixedWithNextNode() {
        return this;
    }
}