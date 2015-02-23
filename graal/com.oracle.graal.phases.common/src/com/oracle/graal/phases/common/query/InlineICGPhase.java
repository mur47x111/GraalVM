package com.oracle.graal.phases.common.query;

import java.util.*;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.common.*;
import com.oracle.graal.debug.*;
import com.oracle.graal.graph.Graph.DuplicationReplacement;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.nodes.virtual.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.query.*;
import com.oracle.graal.phases.tiers.*;

public class InlineICGPhase extends BasePhase<LowTierContext> {

    @Override
    protected void run(StructuredGraph graph, LowTierContext context) {
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase(!GraalOptions.ImmutableCode.getValue());

        for (Node node : graph.getNodes()) {
            if (node instanceof InstrumentationNode) {
                InstrumentationNode instrumentation = (InstrumentationNode) node;
                StructuredGraph icg = instrumentation.getICG().graph();

                canonicalizer.apply(icg, context);
                new DeadCodeEliminationPhase().apply(icg);

                ArrayList<Node> nodes = new ArrayList<>(icg.getNodes().count());
                final StartNode entryPointNode = icg.start();
                FixedNode firstCFGNode = entryPointNode.next();
                ArrayList<ReturnNode> returnNodes = new ArrayList<>(4);

                for (Node icgnode : icg.getNodes()) {
                    if (icgnode == entryPointNode || icgnode == entryPointNode.stateAfter() || icgnode instanceof ParameterNode) {
                        // Do nothing.
                    } else {
                        nodes.add(icgnode);
                        if (icgnode instanceof ReturnNode) {
                            returnNodes.add((ReturnNode) icgnode);
                        }
                    }
                }

                final AbstractBeginNode prevBegin = AbstractBeginNode.prevBegin(instrumentation);
                DuplicationReplacement localReplacement = new DuplicationReplacement() {

                    public Node replacement(Node replacement) {
                        if (replacement instanceof ParameterNode) {
                            ValueNode value = instrumentation.getWeakDependencies().get(((ParameterNode) replacement).index());

                            if (value instanceof VirtualObjectNode) {
                                return graph.unique(new ConstantNode(JavaConstant.NULL_POINTER, value.stamp()));
                            } else {
                                return value;
                            }
                        } else if (replacement == entryPointNode) {
                            return prevBegin;
                        }
                        return replacement;
                    }
                };

                Map<Node, Node> duplicates = graph.addDuplicates(nodes, icg, icg.getNodeCount(), localReplacement);

                FixedNode firstCFGNodeDuplicate = (FixedNode) duplicates.get(firstCFGNode);
                instrumentation.replaceAtPredecessor(firstCFGNodeDuplicate);

                FixedNode n = instrumentation.next();
                instrumentation.setNext(null);

                if (!returnNodes.isEmpty()) {
                    if (returnNodes.size() == 1) {
                        ReturnNode returnNode = (ReturnNode) duplicates.get(returnNodes.get(0));
                        returnNode.replaceAndDelete(n);
                    } else {
                        ArrayList<ReturnNode> returnDuplicates = new ArrayList<>(returnNodes.size());
                        for (ReturnNode returnNode : returnNodes) {
                            returnDuplicates.add((ReturnNode) duplicates.get(returnNode));
                        }
                        AbstractMergeNode merge = graph.add(new MergeNode());

                        for (ReturnNode returnNode : returnDuplicates) {
                            EndNode endNode = graph.add(new EndNode());
                            merge.addForwardEnd(endNode);
                            returnNode.replaceAndDelete(endNode);
                        }

                        merge.setNext(n);
                    }
                }

                for (Node icgnode : icg.getNodes()) {
                    if (icgnode instanceof CompilerDecisionQuery) {
                        ((CompilerDecisionQuery) duplicates.get(icgnode)).inline(instrumentation);
                    }
                }

                String message = instrumentation.toString();
                GraphUtil.killCFG(instrumentation);

                Debug.dump(graph, "After inlining instrumentation " + message);
            }
        }

        canonicalizer.apply(graph, context);
    }

}