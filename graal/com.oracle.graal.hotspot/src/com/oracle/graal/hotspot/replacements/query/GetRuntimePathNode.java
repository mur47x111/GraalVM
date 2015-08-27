package com.oracle.graal.hotspot.replacements.query;

import jdk.internal.jvmci.meta.*;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public final class GetRuntimePathNode extends GraalQueryNode {

    public static final NodeClass<GetRuntimePathNode> TYPE = NodeClass.create(GetRuntimePathNode.class);

    public GetRuntimePathNode() {
        super(TYPE, StampFactory.forKind(Kind.Int));
    }

    @Override
    public void onInlineICG(InstrumentationNode instrumentation, FixedNode position) {
        if (instrumentation.target() instanceof AbstractMergeNode) {
            AbstractMergeNode merge = (AbstractMergeNode) instrumentation.target();
            ValuePhiNode phi = graph().addWithoutUnique(new ValuePhiNode(StampFactory.intValue(), merge));

            for (int i = 0; i < merge.cfgPredecessors().count(); i++) {
                phi.addInput(ConstantNode.forInt(i, merge.graph()));
            }

            graph().replaceFixedWithFloating(this, phi);
        } else {
            graph().replaceFixedWithFloating(this, ConstantNode.forInt(-1, graph()));
        }
    }

    @NodeIntrinsic
    public static native int instantiate();

}
