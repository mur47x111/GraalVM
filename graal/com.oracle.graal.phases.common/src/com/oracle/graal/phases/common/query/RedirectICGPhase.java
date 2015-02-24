package com.oracle.graal.phases.common.query;

import com.oracle.graal.graph.*;
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
        for (Node node : graph.getNodes()) {
            if (node instanceof InstrumentationNode) {
                InstrumentationNode instrumentation = (InstrumentationNode) node;
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

}