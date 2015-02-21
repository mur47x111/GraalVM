package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.hotspot.meta.*;
import com.oracle.graal.nodes.*;

public class CompilerDecisionUtil {

// public static final Stamp STAMP_BOOLEAN = StampFactory.forKind(Kind.Boolean);
// public static final Stamp STAMP_CLASS = exactNonNull(Class.class);

    public static final Stamp STAMP_STRING = exactNonNull(String.class);

    public static Stamp exactNonNull(Class<?> klass) {
        return StampFactory.exactNonNull(HotSpotResolvedJavaType.fromClass(klass));
    }

// public static JavaConstant getMethodFullName(StructuredGraph graph) {
// ResolvedJavaMethod method = graph.method();
//
// // Class name format "L" + full_class_name + ";"
// String className = method.getDeclaringClass().getName();
// String methodname = method.getName();
// // Signature format "HotSpotSignature<" + signature + ">"
// String methodDesc = method.getSignature().toString();
//
// StringBuilder builder = new StringBuilder();
//
// builder.append(className.substring(1, className.length() - 1));
// builder.append(".");
// builder.append(methodname);
// builder.append(methodDesc.substring(methodDesc.indexOf('<') + 1, methodDesc.length() - 1));
//
// return HotSpotObjectConstantImpl.forBoxedValue(Kind.Object, builder.toString());
// }

    public static ConstantNode getMethodFullName(ResolvedJavaMethod method) {
        // Class name format "L" + full_class_name + ";"
        String className = method.getDeclaringClass().getName();
        String methodname = method.getName();
        // Signature format "HotSpotSignature<" + signature + ">"
        String methodDesc = method.getSignature().toString();

        StringBuilder builder = new StringBuilder();

        builder.append(className.substring(1, className.length() - 1));
        builder.append(".");
        builder.append(methodname);
        builder.append(methodDesc.substring(methodDesc.indexOf('<') + 1, methodDesc.length() - 1));

        return new ConstantNode(HotSpotObjectConstantImpl.forBoxedValue(Kind.Object, builder.toString()), STAMP_STRING);
    }

// public static ConstantNode getNull(StructuredGraph graph, Stamp stamp) {
// return graph.unique(ConstantNode.create(Constant.NULL_OBJECT, stamp));
// }
//
// public static ConstantNode toConstantNode(StructuredGraph graph, Constant constant, Stamp stamp)
// {
// return graph.unique(ConstantNode.create(constant, stamp));
// }
//
// public static ConstantNode toConstantNode(StructuredGraph graph, Object object, Stamp stamp) {
// Constant constant = HotSpotObjectConstant.forObject(object);
// return graph.unique(ConstantNode.create(constant, stamp));
// }
//
// public static ConstantNode getConstantNodeClass(StructuredGraph graph, ConstantNode constantNode)
// {
// Class<?> klass = HotSpotObjectConstant.asObject(constantNode.asConstant()).getClass();
// Constant constant = HotSpotObjectConstant.forObject(klass);
// return graph.unique(ConstantNode.create(constant, CompilerDecisionUtil.STAMP_CLASS));
// }
//
// public static Stamp asExactStamp(ConstantNode constantNode) {
// Class<?> klass = (Class<?>) HotSpotObjectConstant.asObject(constantNode.asConstant());
// ResolvedJavaType type = HotSpotResolvedJavaType.fromClass(klass);
// return StampFactory.exact(type);
// }
//
// public static ObjectStamp nullable(AbstractObjectStamp stamp) {
// return new ObjectStamp(stamp.type(), stamp.isExactType(), false, stamp.alwaysNull());
// }

}
