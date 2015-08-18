package com.oracle.graal.phases.common.query;

import java.util.*;

import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.StructuredGraph.AllowAssumptions;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.tiers.*;

public class ExtractICGPhase extends BasePhase<HighTierContext> {

    private static boolean shouldIncludeFloatingNode(Node node, NodeBitMap icgNodes) {
        NodePosIterator iterator = node.inputs().iterator();
        while (iterator.hasNext()) {
            Position pos = iterator.nextPosition();
            if (pos.getInputType() == InputType.Value) {
                continue;
            }
            if (!icgNodes.contains(pos.get(node))) {
                return false;
            }
        }
        return true;
    }

    private static void iterateInput(Node node, NodeBitMap icgNodes) {
        for (Node input : node.inputs()) {
            if (input instanceof FloatingNode) {
                if (!(input instanceof AbstractLocalNode) && shouldIncludeFloatingNode(input, icgNodes)) {
                    icgNodes.mark(input);
                } else {

                }
            } else if (input instanceof CallTargetNode || input instanceof MonitorIdNode) {
                icgNodes.mark(input);
            }
        }
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

    @Override
    protected void run(StructuredGraph graph, HighTierContext context) {
        for (InstrumentationBeginNode icgBegin : graph.getNodes().filter(InstrumentationBeginNode.class)) {
            InstrumentationEndNode icgEnd = null;

            // iterate control flow
            NodeFlood icgFlood = graph.createNodeFlood();
            icgFlood.add(icgBegin);

            for (Node current : icgFlood) {
                if (current instanceof InstrumentationEndNode) {
                    icgEnd = (InstrumentationEndNode) current;
                } else if (current instanceof LoopEndNode) {
                    // TODO (yz) do nothing?
                } else if (current instanceof AbstractEndNode) {
                    icgFlood.add(((AbstractEndNode) current).merge());
                } else {
                    for (Node successor : current.successors()) {
                        icgFlood.add(successor);
                    }
                }
            }

            // iterate data flow
            NodeBitMap icgNodes = icgFlood.getVisited();
            icgNodes.clear(icgBegin);

            NodeBitMap before;

            do {
                before = icgNodes.copy();

                for (Node current : icgNodes) {
                    iterateInput(current, icgNodes);
                }
            } while (!icgNodes.compare(before));

            for (Node current : icgNodes) {
                for (Node input : current.inputs().filter(FrameState.class)) {
                    icgNodes.mark(input);
                }
            }

            FixedNode target = getTarget(icgBegin, icgEnd);
            StructuredGraph icg = new StructuredGraph(AllowAssumptions.YES);
            InstrumentationNode instrumentation = new InstrumentationNode(target, icg, icgBegin.getOffset(), icgBegin.getType());
            graph.addWithoutUnique(instrumentation);
            instrumentation.setStateAfter(icgEnd.stateAfter());

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

            replacements = icg.addDuplicates(icgNodes, graph, icgNodes.count(), replacements);
            icg.start().setNext((FixedNode) replacements.get(icgBegin.next()));
            replacements.get(icgEnd).replaceAtPredecessor(icg.addWithoutUnique(new ReturnNode(null)));

            new DeadCodeEliminationPhase().apply(icg, false);
            Debug.dump(icg, "After extracted ICG at " + instrumentation);

            icgBegin.replaceAtPredecessor(instrumentation);
            FixedNode instrumentationNext = icgEnd.next();
            icgEnd.setNext(null);
            instrumentation.setNext(instrumentationNext);

            GraphUtil.killCFG(icgBegin);
        }

        for (InstrumentationNode instrumentation : graph.getNodes().filter(InstrumentationNode.class)) {
            for (CompilerDecisionQueryNode query : instrumentation.icg().getNodes().filter(CompilerDecisionQueryNode.class)) {
                query.onExtractICG(instrumentation);
            }
        }
    }
}
