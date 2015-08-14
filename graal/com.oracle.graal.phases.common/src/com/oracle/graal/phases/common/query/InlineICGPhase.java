package com.oracle.graal.phases.common.query;

import java.lang.reflect.*;

import jdk.internal.jvmci.debug.*;
import jdk.internal.jvmci.meta.*;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.CallTargetNode.InvokeKind;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.calc.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.inlining.*;
import com.oracle.graal.phases.tiers.*;

public class InlineICGPhase extends BasePhase<LowTierContext> {

    @Override
    protected void run(StructuredGraph graph, LowTierContext context) {
        for (InstrumentationNode instrumentation : graph.getNodes().filter(InstrumentationNode.class)) {
            StructuredGraph icg = instrumentation.icg();
            int length = instrumentation.getWeakDependencies().count();
            ValueNode[] arguments = new ValueNode[length];

            for (int i = 0; i < length; i++) {
                ValueNode value = instrumentation.getWeakDependencies().get(i);
                // TODO (yz) add support for primitives
                if (value == null || value.isDeleted() // null pointer or deleted pointer
                                || !(value instanceof FixedNode || value instanceof FloatingNode) // schedulable
                ) {
                    Stamp stamp = new ObjectStamp(null, false, false, true);
                    ConstantNode constant = new ConstantNode(JavaConstant.NULL_POINTER, stamp);
                    graph.unique(constant);
                    arguments[i] = constant;
                } else {
                    arguments[i] = value;
                }
            }

            try {
                // The following code inserts an invalid InvokeNode and makes use of
                // InliningUtil.inline. The code may fail if InliningUtil.inline checks the argument
                // types.
                // TODO (yz) duplicate the code in InliningUtil.inline
                Method method = DelimitationAPI.class.getDeclaredMethod("instrumentationBegin", int.class);
                ResolvedJavaMethod targetMethod = context.getMetaAccess().lookupJavaMethod(method);
                JavaType type = context.getMetaAccess().lookupJavaType(Void.class);
                MethodCallTargetNode callTarget = graph.add(new MethodCallTargetNode(InvokeKind.Static, targetMethod, arguments, type, null));
                InvokeNode invoke = graph.add(new InvokeNode(callTarget, 0));
                invoke.setStateAfter(null);
                graph.addAfterFixed(instrumentation, invoke);
                InliningUtil.inline(invoke, icg, false, null);

                for (Node node : graph.getNodes()) {
                    if (node instanceof CompilerDecisionQuery) {
                        ((CompilerDecisionQuery) node).onInlineICG(instrumentation);
                    }
                }
            } catch (NoSuchMethodException | SecurityException e) {
                // TODO (yz) exception handling
            }

            GraphUtil.unlinkFixedNode(instrumentation);
            instrumentation.safeDelete();
        }
    }

}
