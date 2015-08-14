package com.oracle.graal.hotspot.replacements.query;

import jdk.internal.jvmci.meta.*;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public final class RuntimePathNode extends FixedWithNextNode implements CompilerDecisionQuery {

    public static final NodeClass<RuntimePathNode> TYPE = NodeClass.create(RuntimePathNode.class);

    public RuntimePathNode() {
        super(TYPE, StampFactory.forKind(Kind.Int));
    }

    @Override
    public void onInlineICG(InstrumentationNode instrumentation) {
        if (instrumentation.target() instanceof AbstractMergeNode) {
            AbstractMergeNode merge = (AbstractMergeNode) instrumentation.target();
            graph().replaceFixedWithFloating(this, CompilerDecisionUtil.createValuePhi(graph(), merge));
        } else {
            graph().replaceFixedWithFloating(this, ConstantNode.forInt(0, graph()));
        }
    }

    @NodeIntrinsic
    public static native int instantiate();

}
