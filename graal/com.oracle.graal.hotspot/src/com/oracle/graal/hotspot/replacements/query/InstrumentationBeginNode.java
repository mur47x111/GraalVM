package com.oracle.graal.hotspot.replacements.query;

import java.util.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.common.query.ExtractICGPhase.ICGBoundary;
import com.oracle.graal.replacements.nodes.*;

@NodeInfo
public class InstrumentationBeginNode extends MacroNode implements CompilerDecisionQuery, ICGBoundary {

    public InstrumentationBeginNode(Invoke invoke) {
        super(invoke);
    }

    @NodeInfo
    static final class TempInputNode extends ValueNode {

        public TempInputNode(Stamp stamp) {
            super(stamp);
        }
    }

    private void redirectInput(StructuredGraph icg, Node node, Map<Node, Node> mapping, Map<TempInputNode, Node> tempInputs) {
        // redirect data dependency to FixedNode or ParameterNode to temporary nodes
        for (Node input : node.inputs()) {
            if (input instanceof FixedNode || input instanceof ParameterNode) {
                TempInputNode tempInput = icg.addWithoutUnique(new TempInputNode(((ValueNode) input).stamp()));
                tempInputs.put(tempInput, input);
                mapping.get(node).replaceFirstInput(mapping.get(input), tempInput);
            } else if (input instanceof FloatingNode || input instanceof CallTargetNode || input instanceof FrameState) {
                redirectInput(icg, input, mapping, tempInputs);
            }
        }
    }

    private boolean getDirection() {
        Node input = inputs().first();
        if (input != null && input instanceof ConstantNode) {
            Constant c = ((ConstantNode) input).getValue();

            if (c instanceof PrimitiveConstant) {
                return ((PrimitiveConstant) c).asBoolean();
            }
        }

        return false;
    }

    public void extract() {
        StructuredGraph graph = graph();
        InstrumentationEndNode originICGEnd = null;

        // duplicate graph
        StructuredGraph icg = new StructuredGraph();
        Map<Node, Node> mapping = graph.copyTo(icg);
        Map<TempInputNode, Node> tempInputs = new HashMap<>();

        // iterate icg
        redirectInput(icg, graph().start(), mapping, tempInputs);
        NodeFlood flood = graph().createNodeFlood();
        flood.add(this);

        for (Node current : flood) {
            if (current instanceof InstrumentationEndNode) {
                originICGEnd = (InstrumentationEndNode) current;
            } else if (current instanceof LoopEndNode) {
                // TODO (yz) do nothing?
            } else if (current instanceof AbstractEndNode) {
                flood.add(((AbstractEndNode) current).merge());
            } else {
                if (!(current instanceof MergeNode)) {
                    redirectInput(icg, current, mapping, tempInputs);
                }

                for (Node successor : current.successors()) {
                    flood.add(successor);
                }
            }
        }

        InstrumentationNode instrumentation = graph().addWithoutUnique(new InstrumentationNode(getDirection() ? next() : (FixedNode) predecessor(), icg));

        // extract icg
        InstrumentationBeginNode icgBegin = (InstrumentationBeginNode) mapping.get(this);
        InstrumentationEndNode icgEnd = (InstrumentationEndNode) mapping.get(originICGEnd);

        StartNode start = icg.start();
        FixedNode icgPred = start.next();

        FixedNode icgWithoutHeader = icgBegin.next();
        icgWithoutHeader.replaceAtPredecessor(null);
        start.setNext(icgWithoutHeader);

        icgEnd.replaceAtPredecessor(icg.addWithoutUnique(new ReturnNode(null)));

        GraphUtil.killCFG(icgPred);
        GraphUtil.killCFG(icgEnd);

        int index = 0;
        Map<Node, ParameterNode> parameters = new HashMap<>();
        // replace temporary nodes with either FixedNode or ParameterNode
        for (TempInputNode tempInput : tempInputs.keySet()) {
            Node input = tempInputs.get(tempInput);
            Node mapInput = mapping.get(input);

            if (input instanceof ParameterNode || mapInput.isDeleted()) {
                ParameterNode parameter = parameters.get(input);

                if (parameter == null) {
                    instrumentation.addInput(input);
                    parameter = icg.addWithoutUnique(new ParameterNode(index++, tempInput.stamp()));
                    parameters.put(input, parameter);
                }

                tempInput.replaceAtUsages(parameter);
            } else {
                tempInput.replaceAtUsages(mapInput);
            }
        }

        Debug.dump(icg, "After extracted ICG starting from " + this);

        // remove icg from original graph
        graph().addBeforeFixed(this, instrumentation);

        FixedNode instrumentationNext = originICGEnd.next();
        originICGEnd.setNext(null);
        instrumentation.setNext(instrumentationNext);

        GraphUtil.killCFG(this);
    }
}