package com.oracle.graal.phases.common.query;

import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.extended.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.query.*;
import com.oracle.graal.phases.tiers.*;

public class RedirectICGPhase extends BasePhase<MidTierContext> {

    @Override
    protected void run(StructuredGraph graph, MidTierContext context) {
        for (InstrumentationNode instrumentation : graph.getNodes().filter(InstrumentationNode.class)) {
            ValueNode target = instrumentation.target();

            if (target instanceof MonitorIdNode) {
                MonitorIdNode id = (MonitorIdNode) target;
                MonitorEnterNode enter = id.usages().filter(MonitorEnterNode.class).first();

                if (enter != null) {
                    instrumentation.replaceFirstInput(id, enter);
                }
            } else if (target instanceof FixedValueAnchorNode) {
                ValueNode value = GraphUtil.unproxify(target);
                instrumentation.replaceFirstInput(target, value);
            }
        }
    }

}