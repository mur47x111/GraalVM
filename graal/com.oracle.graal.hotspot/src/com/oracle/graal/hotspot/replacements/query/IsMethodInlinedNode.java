package com.oracle.graal.hotspot.replacements.query;

import jdk.internal.jvmci.meta.*;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public final class IsMethodInlinedNode extends FixedWithNextNode implements CompilerDecisionQuery {

    public static final NodeClass<IsMethodInlinedNode> TYPE = NodeClass.create(IsMethodInlinedNode.class);

    protected String original;

    public IsMethodInlinedNode() {
        super(TYPE, StampFactory.forKind(Kind.Boolean));
    }

    @Override
    public void onExtractICG(InstrumentationNode instrumentation) {
        original = CompilerDecisionUtil.getMethodFullName(instrumentation.graph().method());
    }

    @Override
    public void onInlineICG(InstrumentationNode instrumentation) {
        String root = CompilerDecisionUtil.getMethodFullName(instrumentation.graph().method());
        graph().replaceFixedWithFloating(this, ConstantNode.forBoolean(!root.equals(original), graph()));
    }

    @NodeIntrinsic
    public static native boolean instantiate();

}