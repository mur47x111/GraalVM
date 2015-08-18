package com.oracle.graal.hotspot.replacements.query;

import jdk.internal.jvmci.hotspot.*;
import jdk.internal.jvmci.meta.*;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.nodes.*;

public class CompilerDecisionUtil {

    public static final Stamp STAMP_STRING = StampFactory.exactNonNull(HotSpotResolvedJavaType.fromClass(String.class));

    public static String getMethodFullName(ResolvedJavaMethod method) {
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

        return builder.toString();
    }

    public static ConstantNode createStringConstant(StructuredGraph graph, String str) {
        return graph.unique(new ConstantNode(HotSpotObjectConstantImpl.forBoxedValue(Kind.Object, str), STAMP_STRING));
    }

}
