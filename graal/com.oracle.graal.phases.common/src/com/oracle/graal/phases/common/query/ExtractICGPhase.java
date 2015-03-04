package com.oracle.graal.phases.common.query;

import java.util.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.common.*;
import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.StructuredGraph.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.*;
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

        public static final NodeClass<TempInputNode> TYPE = NodeClass.create(TempInputNode.class);

        public TempInputNode(Stamp stamp) {
            super(TYPE, stamp);
        }
    }

    private static void redirectInput(StructuredGraph icg, Node node, Map<Node, Node> mapping, Map<TempInputNode, Node> tempInputs) {
        // redirect data dependency to FixedNode or AbstractLocalNode to temporary nodes
        for (Node input : node.inputs()) {
            if (input instanceof FixedNode || input instanceof AbstractLocalNode) {
                TempInputNode tempInput = icg.addWithoutUnique(new TempInputNode(((ValueNode) input).stamp()));
                tempInputs.put(tempInput, input);
                mapping.get(node).replaceFirstInput(mapping.get(input), tempInput);
            } else if (input instanceof FloatingNode || input instanceof CallTargetNode) {
                redirectInput(icg, input, mapping, tempInputs);
            }
        }
    }

    private static int getDirection(FixedWithNextNode begin) {
        Node input = begin.inputs().first();
        if (input != null && input instanceof ConstantNode) {
            Constant c = ((ConstantNode) input).getValue();

            if (c instanceof PrimitiveConstant) {
                return ((PrimitiveConstant) c).asInt();
            }
        }

        return 0;
    }

    private static FixedNode getTarget(FixedWithNextNode begin, FixedWithNextNode end) {
        int direction = getDirection(begin);

        if (direction < 0) {
            Node pred = begin;

            while (direction < 0) {
                pred = pred.predecessor();

                if (pred == null || !(pred instanceof FixedNode)) {
                    return null;
                }

                direction++;
            }

            return (FixedNode) pred;
        } else if (direction > 0) {
            FixedNode next = end;

            while (direction > 0) {
                next = ((FixedWithNextNode) next).next();

                if (next == null || !(next instanceof FixedWithNextNode)) {
                    return null;
                }

                direction--;
            }

            return next;
        }

        return null;
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
                StructuredGraph icg = new StructuredGraph(AllowAssumptions.YES);
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

                FixedNode target = getTarget(originICGBegin, originICGEnd);
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

                // disconnect existing local nodes
                for (Node icgnode : icg.getNodes()) {
                    if (icgnode instanceof AbstractLocalNode) {
                        icgnode.replaceAtUsages(null);
                    }
                    if (icgnode instanceof FrameState) {
                        icgnode.clearInputs();
                    }

                    for (Node input : icgnode.inputs()) {
                        if (input instanceof FrameState && input.isDeleted()) {
                            icgnode.replaceFirstInput(input, null);
                        }
                    }
                }

                int index = 0;
                Map<Node, ParameterNode> parameters = new HashMap<>();
                // replace temporary nodes with either FixedNode or AbstractLocalNode
                for (TempInputNode tempInput : tempInputs.keySet()) {
                    Node input = tempInputs.get(tempInput);
                    Node mapInput = mapping.get(input);
                    Node usage = tempInput.usages().first();

                    if (usage == null) {
                        // do nothing
                    } else if (input instanceof AbstractLocalNode || mapInput.isDeleted()) {
                        // replace with parameter to InstrumentationNode
                        ParameterNode parameter = parameters.get(input);

                        // no need to create new parameter for FrameState
                        if (parameter == null && !(usage instanceof FrameState)) {
                            instrumentation.addInput(input);
                            parameter = icg.addWithoutUnique(new ParameterNode(index++, tempInput.stamp()));
                            parameters.put(input, parameter);
                        }

                        tempInput.replaceAtUsages(parameter);
                    } else {
                        tempInput.replaceAtUsages(mapInput);
                    }
                }

                new DeadCodeEliminationPhase().apply(icg, false);
                new CanonicalizerPhase(!GraalOptions.ImmutableCode.getValue()).apply(icg, context, false);
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