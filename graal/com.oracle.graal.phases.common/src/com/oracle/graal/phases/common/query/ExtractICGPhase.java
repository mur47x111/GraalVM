package com.oracle.graal.phases.common.query;

import java.util.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.inlining.*;
import com.oracle.graal.phases.query.*;
import com.oracle.graal.phases.tiers.*;

public class ExtractICGPhase extends BasePhase<HighTierContext> {

    public interface ICGBoundaryBegin {

        FixedWithNextNode asFixedWithNextNode();

    }

    public interface ICGBoundaryEnd {

        FixedWithNextNode asFixedWithNextNode();

    }

    @NodeInfo
    static final class TempInputNode extends ValueNode {

        public TempInputNode(Stamp stamp) {
            super(stamp);
        }
    }

    private static void redirectInput(StructuredGraph icg, Node node, Map<Node, Node> mapping, Map<TempInputNode, Node> tempInputs) {
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

    private static boolean getDirection(FixedWithNextNode begin) {
        Node input = begin.inputs().first();
        if (input != null && input instanceof ConstantNode) {
            Constant c = ((ConstantNode) input).getValue();

            if (c instanceof PrimitiveConstant) {
                return ((PrimitiveConstant) c).asBoolean();
            }
        }

        return false;
    }

    @Override
    protected void run(StructuredGraph graph, HighTierContext context) {
        Replacements cr = context.getReplacements();

        // first iteration: replace query invocation with macro nodes
        for (InvokeNode invokeNode : graph.getNodes().filter(InvokeNode.class)) {
            ResolvedJavaMethod targetMethod = invokeNode.callTarget().targetMethod();
            Class<? extends FixedWithNextNode> macroNodeClass = InliningUtil.getMacroNodeClass(cr, targetMethod);

            if (macroNodeClass != null && CompilerDecisionQuery.class.isAssignableFrom(macroNodeClass)) {
                InliningUtil.inlineMacroNode(invokeNode, targetMethod, macroNodeClass);
            }
        }

        // second iteration: resolve if possible
        for (Node node : graph.getNodes()) {
            if (node instanceof CompilerDecisionQuery) {
                ConstantNode c = ((CompilerDecisionQuery) node).resolve();

                if (c != null) {
                    graph.replaceFixedWithFloating((FixedWithNextNode) node, c);
                }
            }
        }

        // third iteration: extract ICG
        for (Node node : graph.getNodes()) {
            if (node instanceof ICGBoundaryBegin) {
                FixedWithNextNode originICGBegin = ((ICGBoundaryBegin) node).asFixedWithNextNode();
                FixedWithNextNode originICGEnd = null;

                // duplicate graph
                StructuredGraph icg = new StructuredGraph();
                Map<Node, Node> mapping = graph.copyTo(icg);
                Map<TempInputNode, Node> tempInputs = new HashMap<>();

                // iterate icg
                redirectInput(icg, graph.start(), mapping, tempInputs);
                NodeFlood flood = graph.createNodeFlood();
                flood.add(originICGBegin);

                for (Node current : flood) {
                    if (current instanceof ICGBoundaryEnd) {
                        originICGEnd = ((ICGBoundaryEnd) current).asFixedWithNextNode();
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

                FixedNode target = getDirection(originICGBegin) ? originICGEnd.next() : (FixedNode) originICGBegin.predecessor();
                InstrumentationNode instrumentation = graph.addWithoutUnique(new InstrumentationNode(target, icg));

                // extract icg
                FixedWithNextNode icgBegin = (FixedWithNextNode) mapping.get(originICGBegin);
                FixedWithNextNode icgEnd = (FixedWithNextNode) mapping.get(originICGEnd);

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

                Debug.dump(icg, "After extracted ICG starting from " + node);

                // remove icg from original graph
                graph.addBeforeFixed(originICGBegin, instrumentation);

                FixedNode instrumentationNext = originICGEnd.next();
                originICGEnd.setNext(null);
                instrumentation.setNext(instrumentationNext);

                GraphUtil.killCFG(originICGBegin);
            }
        }
    }

}