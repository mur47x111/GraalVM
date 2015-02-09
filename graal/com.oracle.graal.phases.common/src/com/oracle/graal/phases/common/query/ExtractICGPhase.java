package com.oracle.graal.phases.common.query;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.inlining.*;
import com.oracle.graal.phases.tiers.*;

public class ExtractICGPhase extends BasePhase<HighTierContext> {

    public interface CompilerDecisionQuery {

        default void resolve() {
        }

    }

    @Override
    protected void run(StructuredGraph graph, HighTierContext context) {
        Replacements cr = context.getReplacements();

        for (InvokeNode invokeNode : graph.getNodes().filter(InvokeNode.class)) {
            ResolvedJavaMethod targetMethod = invokeNode.callTarget().targetMethod();
            Class<? extends FixedWithNextNode> macroNodeClass = InliningUtil.getMacroNodeClass(cr, targetMethod);

            if (macroNodeClass != null && CompilerDecisionQuery.class.isAssignableFrom(macroNodeClass)) {
                InliningUtil.inlineMacroNode(invokeNode, targetMethod, macroNodeClass);
            }
        }

        for (Node node : graph.getNodes()) {
            if (node instanceof CompilerDecisionQuery) {
                ((CompilerDecisionQuery) node).resolve();
            }
        }
    }

}