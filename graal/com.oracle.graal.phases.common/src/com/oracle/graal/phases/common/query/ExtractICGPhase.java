package com.oracle.graal.phases.common.query;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.inlining.*;
import com.oracle.graal.phases.tiers.*;

public class ExtractICGPhase extends BasePhase<HighTierContext> {

    public interface ICGBoundary {

        default void extract() {
        }

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
            if (node instanceof ICGBoundary) {
                ((ICGBoundary) node).extract();
            }
        }
    }

}