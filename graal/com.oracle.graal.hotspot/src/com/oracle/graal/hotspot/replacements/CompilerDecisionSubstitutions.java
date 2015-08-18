package com.oracle.graal.hotspot.replacements;

import jdk.internal.jvmci.debug.*;

import com.oracle.graal.api.replacements.*;
import com.oracle.graal.hotspot.replacements.query.*;

@ClassSubstitution(CompilerDecision.class)
public class CompilerDecisionSubstitutions {

    @MethodSubstitution(isStatic = true)
    public static boolean isMethodCompiled() {
        return true;
    }

    @MethodSubstitution(isStatic = true)
    public static boolean isMethodInlined() {
        return IsMethodInlinedNode.instantiate();
    }

    @MethodSubstitution(isStatic = true)
    public static String getRootName() {
        return GetRootNameNode.instantiate();
    }

    @MethodSubstitution(isStatic = true)
    public static int getAllocationType() {
        return GetRuntimePathNode.instantiate();
    }

    @MethodSubstitution(isStatic = true)
    public static int getLockType() {
        return GetRuntimePathNode.instantiate();
    }

    @MethodSubstitution(isStatic = true)
    public static int getDeoptReason() {
        return GetDeoptReasonNode.instantiate();
    }

    @MethodSubstitution(isStatic = true)
    public static int getDeoptAction() {
        return GetDeoptActionNode.instantiate();
    }

    @MethodSubstitution(isStatic = true)
    public static int getDeoptBCI() {
        return GetDeoptBCINode.instantiate();
    }

}
