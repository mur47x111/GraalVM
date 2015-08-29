package com.oracle.graal.hotspot.replacements.query;

import jdk.internal.jvmci.hotspot.*;
import jdk.internal.jvmci.meta.*;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public final class GetRootNameNode extends GraalQueryNode {

    public static final NodeClass<GetRootNameNode> TYPE = NodeClass.create(GetRootNameNode.class);

    public GetRootNameNode() {
        super(TYPE, StampFactory.exactNonNull(HotSpotResolvedJavaType.fromClass(String.class)));
    }

    @Override
    public void onInlineICG(InstrumentationNode instrumentation, FixedNode position) {
        ResolvedJavaMethod method = graph().method();
        String root = method.getDeclaringClass().toJavaName() + "." + method.getName() + method.getSignature().toMethodDescriptor();
        Constant constant = HotSpotObjectConstantImpl.forBoxedValue(Kind.Object, root);
        ConstantNode constantNode = graph().unique(new ConstantNode(constant, stamp()));
        graph().replaceFixedWithFloating(this, constantNode);
    }

    @NodeIntrinsic
    public static native String instantiate();

}
