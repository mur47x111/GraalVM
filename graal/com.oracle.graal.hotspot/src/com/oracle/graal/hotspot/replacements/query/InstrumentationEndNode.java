package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.ExtractICGPhase.*;
import com.oracle.graal.replacements.nodes.*;

@NodeInfo
public class InstrumentationEndNode extends MacroNode implements CompilerDecisionQuery {

    public InstrumentationEndNode(Invoke invoke) {
        super(invoke);
    }

}