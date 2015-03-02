package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.hotspot.meta.*;
import com.oracle.graal.nodes.*;

public class CompilerDecisionUtil {

    public static final Stamp STAMP_STRING = exactNonNull(String.class);

    public static Stamp exactNonNull(Class<?> klass) {
        return StampFactory.exactNonNull(HotSpotResolvedJavaType.fromClass(klass));
    }

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

    public static ValuePhiNode createValuePhi(StructuredGraph graph, AbstractMergeNode merge) {
        ValuePhiNode path = graph.addWithoutUnique(new ValuePhiNode(StampFactory.intValue(), merge));

        for (int i = 0; i < merge.cfgPredecessors().count(); i++) {
            path.addInput(ConstantNode.forInt(i, merge.graph()));
        }

        return path;
    }

}
