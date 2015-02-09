package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.compiler.common.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.common.query.ExtractICGPhase.CompilerDecisionQuery;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.replacements.nodes.*;

@NodeInfo
public class InstrumentationBeginNode extends MacroNode implements CompilerDecisionQuery {

    public InstrumentationBeginNode(Invoke invoke) {
        super(invoke);
    }

    private InstrumentationEndNode pair() {
        // TODO check dominating
        NodeFlood flood = graph().createNodeFlood();
        flood.add(this);

        for (Node current : flood) {
            if (current instanceof InstrumentationEndNode) {
                return (InstrumentationEndNode) current;
            } else if (current instanceof LoopEndNode) {
                continue;
            } else if (current instanceof AbstractEndNode) {
                flood.add(((AbstractEndNode) current).merge());
            } else {
                for (Node successor : current.successors()) {
                    flood.add(successor);
                }
            }
        }

        throw new GraalInternalError("invoke instrumentationBegin without instrumentationEnd");
    }

    public void resolve() {
        InstrumentationNode instrumentation = graph().addWithoutUnique(new InstrumentationNode());
        graph().addBeforeFixed(this, instrumentation);

        InstrumentationEndNode instrumentationEnd = pair();

        FixedNode instrumentationNext = instrumentationEnd.next();
        instrumentationEnd.setNext(null);
        instrumentation.setNext(instrumentationNext);

        GraphUtil.killCFG(this);
    }

}