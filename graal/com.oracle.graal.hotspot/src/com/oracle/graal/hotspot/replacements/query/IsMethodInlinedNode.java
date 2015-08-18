package com.oracle.graal.hotspot.replacements.query;

import jdk.internal.jvmci.meta.*;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public final class IsMethodInlinedNode extends CompilerDecisionQueryNode {

    public static final NodeClass<IsMethodInlinedNode> TYPE = NodeClass.create(IsMethodInlinedNode.class);

    protected int original;

    public IsMethodInlinedNode() {
        super(TYPE, StampFactory.forKind(Kind.Boolean));
    }

    @Override
    public void onExtractICG(InstrumentationNode instrumentation) {
        original = System.identityHashCode(instrumentation.graph());
    }

    @Override
    public void onInlineICG(InstrumentationNode instrumentation, FixedNode position) {
        graph().replaceFixedWithFloating(this, ConstantNode.forBoolean(original != System.identityHashCode(instrumentation.graph()), graph()));
    }

    @Override
    protected void replaceWithDefault() {
        graph().replaceFixedWithFloating(this, ConstantNode.forBoolean(false, graph()));
    }

    @NodeIntrinsic
    public static native boolean instantiate();

}