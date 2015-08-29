package com.oracle.graal.phases.common.query;

import java.util.*;

import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.tiers.*;

public class ExtractICGPhase extends BasePhase<HighTierContext> {

    private boolean isTopLevel;

    public ExtractICGPhase(boolean isTopLevel) {
        this.isTopLevel = isTopLevel;
    }

    private static boolean shouldIncludeInput(Node node, NodeBitMap cfgNodes) {
        if (node instanceof FloatingNode && !(node instanceof AbstractLocalNode)) {
            NodePosIterator iterator = node.inputs().iterator();
            while (iterator.hasNext()) {
                Position pos = iterator.nextPosition();
                if (pos.getInputType() == InputType.Value) {
                    continue;
                }
                if (!cfgNodes.isMarked(pos.get(node))) {
                    return false;
                }
            }
            return true;
        }
        if (node instanceof CallTargetNode || node instanceof MonitorIdNode || node instanceof FrameState) {
            return true;
        }
        return false;
    }

    private static FixedNode getTarget(InstrumentationBeginNode icgBegin, InstrumentationEndNode icgEnd) {
        int offset = icgBegin.getOffset();
        if (offset < 0) {
            Node pred = icgBegin;
            while (offset < 0) {
                pred = pred.predecessor();
                if (pred == null || !(pred instanceof FixedNode)) {
                    return null;
                }
                offset++;
            }
            return (FixedNode) pred;
        } else if (offset > 0) {
            FixedNode next = icgEnd;
            while (offset > 0) {
                next = ((FixedWithNextNode) next).next();
                if (next == null || !(next instanceof FixedWithNextNode)) {
                    return null;
                }
                offset--;
            }
            return next;
        }
        return null;
    }

    private static InstrumentationNode createInstrumentationNode(InstrumentationBeginNode icgBegin, InstrumentationEndNode icgEnd, NodeBitMap icgNodes) {
        FixedNode target = getTarget(icgBegin, icgEnd);
        InstrumentationNode instrumentation = new InstrumentationNode(target, icgBegin.getOffset(), icgBegin.getType());
        icgBegin.graph().addWithoutUnique(instrumentation);
        StructuredGraph icg = instrumentation.icg();

        Map<Node, Node> replacements = Node.newMap();
        int index = 0;

        for (Node current : icgNodes) {
            if (current instanceof FrameState) {
                continue;
            }
            for (Node input : current.inputs()) {
                if (!(input instanceof ValueNode)) {
                    continue;
                }
                if (!icgNodes.isMarked(input) && !replacements.containsKey(input)) {
                    ParameterNode parameter = new ParameterNode(index++, ((ValueNode) input).stamp());
                    icg.addWithoutUnique(parameter);
                    replacements.put(input, parameter);
                    instrumentation.addInput(input);
                }
            }
        }

        replacements = icg.addDuplicates(icgNodes, icgBegin.graph(), icgNodes.count(), replacements);
        icg.start().setNext((FixedNode) replacements.get(icgBegin.next()));
        replacements.get(icgEnd).replaceAtPredecessor(icg.addWithoutUnique(new ReturnNode(null)));

        new DeadCodeEliminationPhase().apply(icg, false);
        Debug.dump(icg, "After extracted ICG at " + instrumentation);
        return instrumentation;
    }

    @Override
    protected void run(StructuredGraph graph, HighTierContext context) {
        for (InstrumentationBeginNode icgBegin : graph.getNodes().filter(InstrumentationBeginNode.class)) {
            InstrumentationEndNode icgEnd = null;
            // iterate icg nodes via both control flow and data flow
            NodeFlood icgCFG = graph.createNodeFlood();
            icgCFG.add(icgBegin.next());
            for (Node current : icgCFG) {
                if (current instanceof InstrumentationEndNode) {
                    icgEnd = (InstrumentationEndNode) current;
                } else if (current instanceof LoopEndNode) {
                    // do nothing
                } else if (current instanceof AbstractEndNode) {
                    icgCFG.add(((AbstractEndNode) current).merge());
                } else {
                    icgCFG.addAll(current.successors());
                }
            }

            if (icgBegin.getType() == 0 || isTopLevel) {
                NodeBitMap cfgNodes = icgCFG.getVisited();
                NodeFlood icgDFG = graph.createNodeFlood();
                icgDFG.addAll(cfgNodes);
                for (Node current : icgDFG) {
                    if (current instanceof FrameState) {
                        continue;
                    }
                    for (Node input : current.inputs()) {
                        if (shouldIncludeInput(input, cfgNodes)) {
                            icgDFG.add(input);
                        }
                    }
                }
                InstrumentationNode instrumentation = createInstrumentationNode(icgBegin, icgEnd, icgDFG.getVisited());
                graph.addBeforeFixed(icgBegin, instrumentation);
            }
            FixedNode next = icgEnd.next();
            icgEnd.setNext(null);
            icgBegin.replaceAtPredecessor(next);
            GraphUtil.killCFG(icgBegin);
        }

        for (InstrumentationNode instrumentation : graph.getNodes().filter(InstrumentationNode.class)) {
            for (GraalQueryNode query : instrumentation.icg().getNodes().filter(GraalQueryNode.class)) {
                query.onExtractICG(instrumentation);
            }
        }
    }
}
