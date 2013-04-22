/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.codegen.processor.node;

import static com.oracle.truffle.codegen.processor.Utils.*;
import static javax.lang.model.element.Modifier.*;

import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

import com.oracle.truffle.api.codegen.*;
import com.oracle.truffle.codegen.processor.*;
import com.oracle.truffle.codegen.processor.ast.*;
import com.oracle.truffle.codegen.processor.node.NodeChildData.Cardinality;
import com.oracle.truffle.codegen.processor.node.NodeChildData.ExecutionKind;
import com.oracle.truffle.codegen.processor.template.*;
import com.oracle.truffle.codegen.processor.typesystem.*;

public class NodeCodeGenerator extends CompilationUnitFactory<NodeData> {

    private static final String THIS_NODE_LOCAL_VAR_NAME = "thisNode";

    public NodeCodeGenerator(ProcessorContext context) {
        super(context);
    }

    private TypeMirror getUnexpectedValueException() {
        return getContext().getTruffleTypes().getUnexpectedValueException();
    }

    private static String factoryClassName(NodeData node) {
        return node.getNodeId() + "Factory";
    }

    private static String nodeSpecializationClassName(SpecializationData specialization) {
        String nodeid = specialization.getNode().getNodeId();
        if (nodeid.endsWith("Node") && !nodeid.equals("Node")) {
            nodeid = nodeid.substring(0, nodeid.length() - 4);
        }

        String name = Utils.firstLetterUpperCase(nodeid);
        name += Utils.firstLetterUpperCase(specialization.getId());
        name += "Node";
        return name;
    }

    private static String valueName(ActualParameter param) {
        return param.getLocalName();
    }

    private static String castValueName(ActualParameter parameter) {
        return valueName(parameter) + "Cast";
    }

    private void addInternalValueParameters(CodeExecutableElement method, TemplateMethod specialization, boolean forceFrame) {
        if (forceFrame && specialization.getSpecification().findParameterSpec("frame") != null) {
            method.addParameter(new CodeVariableElement(getContext().getTruffleTypes().getFrame(), "frameValue"));
        }
        for (ActualParameter parameter : specialization.getParameters()) {
            ParameterSpec spec = parameter.getSpecification();
            if (forceFrame && spec.getName().equals("frame")) {
                continue;
            }
            if (spec.isLocal()) {
                continue;
            }

            method.addParameter(new CodeVariableElement(parameter.getType(), valueName(parameter)));
        }
    }

    private static void addInternalValueParameterNames(CodeTreeBuilder builder, TemplateMethod source, TemplateMethod specialization, String unexpectedValueName, boolean forceFrame,
                    boolean includeImplicit) {
        if (forceFrame && specialization.getSpecification().findParameterSpec("frame") != null) {
            builder.string("frameValue");
        }
        for (ActualParameter parameter : specialization.getParameters()) {
            ParameterSpec spec = parameter.getSpecification();
            if (forceFrame && spec.getName().equals("frame")) {
                continue;
            }

            if (!includeImplicit && (parameter.isImplicit())) {
                continue;
            }
            if (parameter.getSpecification().isLocal()) {
                continue;
            }

            ActualParameter sourceParameter = source.findParameter(parameter.getLocalName());

            if (unexpectedValueName != null && parameter.getLocalName().equals(unexpectedValueName)) {
                builder.cast(parameter.getType(), CodeTreeBuilder.singleString("ex.getResult()"));
            } else if (sourceParameter != null) {
                builder.string(valueName(sourceParameter, parameter));
            } else {
                builder.string(valueName(parameter));
            }
        }
    }

    private static String valueName(ActualParameter sourceParameter, ActualParameter targetParameter) {
        if (sourceParameter != null) {
            if (!sourceParameter.getSpecification().isSignature()) {
                return valueName(targetParameter);
            } else if (sourceParameter.getTypeSystemType() != null && targetParameter.getTypeSystemType() != null) {
                if (sourceParameter.getTypeSystemType().needsCastTo(targetParameter.getTypeSystemType())) {
                    return castValueName(targetParameter);
                }
            }
            return valueName(targetParameter);
        } else {
            return valueName(targetParameter);
        }
    }

    private CodeTree createTemplateMethodCall(CodeTreeBuilder parent, TemplateMethod sourceMethod, TemplateMethod targetMethod, String unexpectedValueName) {
        CodeTreeBuilder builder = parent.create();

        boolean castedValues = sourceMethod != targetMethod;

        builder.startGroup();
        ExecutableElement method = targetMethod.getMethod();
        if (method == null) {
            throw new IllegalStateException("Cannot call synthetic operation methods.");
        }
        TypeElement targetClass = Utils.findNearestEnclosingType(method.getEnclosingElement());
        NodeData node = (NodeData) targetMethod.getTemplate();

        boolean accessible = targetMethod.canBeAccessedByInstanceOf(getContext(), node.getNodeType());
        if (accessible) {
            if (builder.findMethod().getModifiers().contains(STATIC)) {
                if (method.getModifiers().contains(STATIC)) {
                    builder.type(targetClass.asType());
                } else {
                    builder.string(THIS_NODE_LOCAL_VAR_NAME);
                }
            } else {
                if (targetMethod instanceof ExecutableTypeData) {
                    builder.string("this");
                } else {
                    builder.string("super");
                }
            }
        } else {
            if (method.getModifiers().contains(STATIC)) {
                builder.type(targetClass.asType());
            } else {
                ActualParameter parameter = null;
                for (ActualParameter searchParameter : targetMethod.getParameters()) {
                    if (searchParameter.getSpecification().isSignature()) {
                        parameter = searchParameter;
                        break;
                    }
                }
                ActualParameter sourceParameter = sourceMethod.findParameter(parameter.getLocalName());
                assert parameter != null;

                if (castedValues && sourceParameter != null) {
                    builder.string(valueName(sourceParameter, parameter));
                } else {
                    builder.string(valueName(parameter));
                }
            }
        }
        builder.string(".");
        builder.startCall(method.getSimpleName().toString());

        for (ActualParameter targetParameter : targetMethod.getParameters()) {
            ActualParameter valueParameter = sourceMethod.findParameter(targetParameter.getLocalName());
            if (valueParameter == null) {
                valueParameter = targetParameter;
            }
            TypeData targetType = targetParameter.getTypeSystemType();

            if (targetParameter.isImplicit() || valueParameter.isImplicit()) {
                continue;
            }

            TypeData valueType = null;
            if (valueParameter != null) {
                valueType = valueParameter.getTypeSystemType();
            }

            if (targetParameter.getSpecification().isLocal()) {
                builder.startGroup();
                if (builder.findMethod().getModifiers().contains(Modifier.STATIC)) {
                    builder.string(THIS_NODE_LOCAL_VAR_NAME).string(".");
                } else {
                    builder.string("this.");
                }
                builder.string(targetParameter.getSpecification().getName());
                builder.end();
            } else if (unexpectedValueName != null && targetParameter.getLocalName().equals(unexpectedValueName)) {
                builder.string("ex.getResult()");
            } else if (targetType == null || targetType.isGeneric() || (valueType != null && valueType.equalsType(targetType))) {
                builder.startGroup();

                if (valueType != null && sourceMethod.getMethodName().equals(targetMethod.getMethodName()) && !valueType.isGeneric() && targetType.isGeneric()) {
                    builder.string("(");
                    builder.type(targetType.getPrimitiveType());
                    builder.string(") ");
                }
                builder.string(valueName(targetParameter));
                builder.end();
            } else {
                builder.string(castValueName(targetParameter));
            }
        }

        builder.end().end();

        return builder.getRoot();
    }

    private static String genClassName(NodeData node) {
        String nodeid = node.getNodeId();
        if (nodeid.endsWith("Node") && !nodeid.equals("Node")) {
            nodeid = nodeid.substring(0, nodeid.length() - 4);
        }
        String name = Utils.firstLetterUpperCase(nodeid);
        name += "GenNode";
        return name;
    }

    private String generatedGenericMethodName(SpecializationData specialization) {
        final String prefix = "generic";

        if (specialization == null) {
            return prefix;
        }

        if (!specialization.getNode().needsRewrites(context)) {
            return prefix;
        }

        SpecializationData prev = null;
        for (SpecializationData current : specialization.getNode().getSpecializations()) {
            if (specialization == current) {
                if (prev == null || prev.isUninitialized()) {
                    return prefix;
                } else {
                    return prefix + current.getId();
                }
            }
            prev = current;
        }
        return prefix;
    }

    private static CodeTree createCallTypeSystemMethod(ProcessorContext context, CodeTreeBuilder parent, NodeData node, String methodName, CodeTree value) {
        CodeTreeBuilder builder = new CodeTreeBuilder(parent);
        startCallTypeSystemMethod(context, builder, node, methodName);
        builder.tree(value);
        builder.end().end();
        return builder.getRoot();
    }

    private static void startCallTypeSystemMethod(ProcessorContext context, CodeTreeBuilder body, NodeData node, String methodName) {
        VariableElement singleton = TypeSystemCodeGenerator.findSingleton(context, node.getTypeSystem());
        assert singleton != null;

        body.startGroup();
        body.staticReference(singleton.getEnclosingElement().asType(), singleton.getSimpleName().toString());
        body.string(".").startCall(methodName);
    }

    private CodeTree createGuardAndCast(CodeTreeBuilder parent, String conditionPrefix, SpecializationData sourceSpecialization, SpecializationData targetSpecialization, boolean castValues,
                    CodeTree guardedStatements, CodeTree elseStatements) {

        NodeData node = targetSpecialization.getNode();
        CodeTreeBuilder builder = new CodeTreeBuilder(parent);
        CodeTree implicitGuards = createImplicitGuards(parent, conditionPrefix, sourceSpecialization, targetSpecialization);
        CodeTree explicitGuards = createExplicitGuards(parent, implicitGuards == null ? conditionPrefix : null, sourceSpecialization, targetSpecialization);

        Set<String> valuesNeedsCast;
        if (castValues) {
            // cast all
            valuesNeedsCast = null;
        } else {
            // find out which values needs a cast
            valuesNeedsCast = new HashSet<>();
            for (GuardData guard : targetSpecialization.getGuards()) {
                for (ActualParameter parameter : guard.getParameters()) {
                    NodeChildData field = node.findChild(parameter.getSpecification().getName());
                    if (field == null) {
                        continue;
                    }
                    TypeData typeData = parameter.getTypeSystemType();
                    if (typeData != null && !typeData.isGeneric()) {
                        valuesNeedsCast.add(parameter.getLocalName());
                    }
                }
            }
        }

        int ifCount = 0;

        if (implicitGuards != null) {
            builder.startIf();
            builder.tree(implicitGuards);
            builder.end();
            builder.startBlock();
            ifCount++;
        }

        builder.tree(createCasts(parent, valuesNeedsCast, sourceSpecialization, targetSpecialization));

        if (explicitGuards != null) {
            builder.startIf();
            builder.tree(explicitGuards);
            builder.end();
            builder.startBlock();
            ifCount++;
        }

        if (implicitGuards == null && explicitGuards == null && conditionPrefix != null && !conditionPrefix.isEmpty()) {
            builder.startIf();
            builder.string(conditionPrefix);
            builder.end().startBlock();
            ifCount++;
        }

        builder.tree(guardedStatements);

        builder.end(ifCount);
        if (elseStatements != null && ifCount > 0) {
            builder.tree(elseStatements);
        }
        return builder.getRoot();
    }

    private CodeTree createExplicitGuards(CodeTreeBuilder parent, String conditionPrefix, SpecializationData valueSpecialization, SpecializationData guardedSpecialization) {
        CodeTreeBuilder builder = new CodeTreeBuilder(parent);
        String andOperator = conditionPrefix != null ? conditionPrefix + " && " : "";
        if (guardedSpecialization.getGuards().size() > 0) {
            // Explicitly specified guards
            for (GuardData guard : guardedSpecialization.getGuards()) {
                builder.string(andOperator);
                builder.tree(createTemplateMethodCall(parent, valueSpecialization, guard, null));
                andOperator = " && ";
            }
        }

        return builder.isEmpty() ? null : builder.getRoot();
    }

    private CodeTree createCasts(CodeTreeBuilder parent, Set<String> castWhiteList, SpecializationData valueSpecialization, SpecializationData guardedSpecialization) {
        CodeTreeBuilder builder = new CodeTreeBuilder(parent);
        // Implict guards based on method signature
        for (ActualParameter guardedParam : guardedSpecialization.getParameters()) {
            NodeChildData field = guardedSpecialization.getNode().findChild(guardedParam.getSpecification().getName());
            if (field == null) {
                continue;
            }
            ActualParameter valueParam = valueSpecialization.findParameter(guardedParam.getLocalName());

            if (castWhiteList != null && !castWhiteList.contains(guardedParam.getLocalName())) {
                continue;
            }

            CodeTree cast = createCast(parent, field, valueParam, guardedParam);
            if (cast == null) {
                continue;
            }
            builder.tree(cast);
        }

        return builder.getRoot();
    }

    private CodeTree createImplicitGuards(CodeTreeBuilder parent, String conditionPrefix, SpecializationData valueSpecialization, SpecializationData guardedSpecialization) {
        CodeTreeBuilder builder = new CodeTreeBuilder(parent);
        // Implict guards based on method signature
        String andOperator = conditionPrefix != null ? conditionPrefix + " && " : "";
        for (ActualParameter guardedParam : guardedSpecialization.getParameters()) {
            NodeChildData field = guardedSpecialization.getNode().findChild(guardedParam.getSpecification().getName());
            if (field == null) {
                continue;
            }
            ActualParameter valueParam = valueSpecialization.findParameter(guardedParam.getLocalName());

            CodeTree implicitGuard = createImplicitGuard(builder, field, valueParam, guardedParam);
            if (implicitGuard == null) {
                continue;
            }

            builder.string(andOperator);
            builder.tree(implicitGuard);
            andOperator = " && ";
        }

        return builder.isEmpty() ? null : builder.getRoot();
    }

    private CodeTree createImplicitGuard(CodeTreeBuilder parent, NodeChildData field, ActualParameter source, ActualParameter target) {
        NodeData node = field.getNodeData();
        CodeTreeBuilder builder = new CodeTreeBuilder(parent);

        TypeData targetType = target.getTypeSystemType();
        TypeData sourceType = source.getTypeSystemType();

        if (targetType.equalsType(sourceType) || targetType.isGeneric()) {
            return null;
        }

        builder.startGroup();

        if (field.isShortCircuit()) {
            ActualParameter shortCircuit = target.getPreviousParameter();
            assert shortCircuit != null;
            builder.string("(");
            builder.string("!").string(valueName(shortCircuit));
            builder.string(" || ");
        }

        startCallTypeSystemMethod(getContext(), builder, node, TypeSystemCodeGenerator.isTypeMethodName(target.getTypeSystemType()));
        builder.string(valueName(source));
        builder.end().end(); // call

        if (field.isShortCircuit()) {
            builder.string(")");
        }

        builder.end(); // group

        return builder.getRoot();
    }

    private CodeTree createCast(CodeTreeBuilder parent, NodeChildData field, ActualParameter source, ActualParameter target) {
        NodeData node = field.getNodeData();
        TypeData sourceType = source.getTypeSystemType();
        TypeData targetType = target.getTypeSystemType();

        if (!sourceType.needsCastTo(targetType)) {
            return null;
        }

        CodeTree condition = null;
        if (field.isShortCircuit()) {
            ActualParameter shortCircuit = target.getPreviousParameter();
            assert shortCircuit != null;
            condition = CodeTreeBuilder.singleString(valueName(shortCircuit));
        }

        CodeTree value = createCallTypeSystemMethod(context, parent, node, TypeSystemCodeGenerator.asTypeMethodName(targetType), CodeTreeBuilder.singleString(valueName(target)));

        return createLazyAssignment(parent, castValueName(target), target.getType(), condition, value);
    }

    /**
     * <pre>
     * variant1 $condition != null
     * 
     * $type $name = defaultValue($type);
     * if ($condition) {
     *     $name = $value;
     * }
     * 
     * variant2 $condition != null
     * $type $name = $value;
     * </pre>
     * 
     * .
     */
    private static CodeTree createLazyAssignment(CodeTreeBuilder parent, String name, TypeMirror type, CodeTree condition, CodeTree value) {
        CodeTreeBuilder builder = new CodeTreeBuilder(parent);
        if (condition == null) {
            builder.declaration(type, name, value);
        } else {
            builder.declaration(type, name, new CodeTreeBuilder(parent).defaultValue(type).getRoot());

            builder.startIf().tree(condition).end();
            builder.startBlock();
            builder.startStatement();
            builder.string(name);
            builder.string(" = ");
            builder.tree(value);
            builder.end(); // statement
            builder.end(); // block
        }
        return builder.getRoot();
    }

    private void emitEncounteredSynthetic(CodeTreeBuilder builder) {
        builder.startThrow().startNew(getContext().getType(UnsupportedOperationException.class)).end().end();
    }

    private static List<ExecutableElement> findUserConstructors(TypeMirror nodeType) {
        List<ExecutableElement> constructors = new ArrayList<>();
        for (ExecutableElement constructor : ElementFilter.constructorsIn(Utils.fromTypeMirror(nodeType).getEnclosedElements())) {
            if (constructor.getModifiers().contains(PRIVATE)) {
                continue;
            }
            if (isCopyConstructor(constructor)) {
                continue;
            }
            constructors.add(constructor);
        }

        if (constructors.isEmpty()) {
            constructors.add(new CodeExecutableElement(null, Utils.getSimpleName(nodeType)));
        }

        return constructors;
    }

    private static boolean isCopyConstructor(ExecutableElement element) {
        if (element.getParameters().size() != 1) {
            return false;
        }
        VariableElement var = element.getParameters().get(0);
        TypeElement type = Utils.findNearestEnclosingType(var);

        if (!Utils.typeEquals(var.asType(), type.asType())) {
            return false;
        }
        return true;
    }

    @Override
    protected void createChildren(NodeData node) {
        Map<NodeData, List<TypeElement>> childTypes = new LinkedHashMap<>();
        if (node.getDeclaredNodes() != null && !node.getDeclaredNodes().isEmpty()) {
            for (NodeData nodeChild : node.getDeclaredNodes()) {
                NodeCodeGenerator generator = new NodeCodeGenerator(getContext());
                childTypes.put(nodeChild, generator.process(null, nodeChild).getEnclosedElements());
            }
        }

        if (node.needsFactory() || node.getNodeDeclaringChildren().size() > 0) {
            add(new NodeFactoryFactory(context, childTypes), node);
        }
    }

    private class NodeGenFactory extends ClassElementFactory<NodeData> {

        public NodeGenFactory(ProcessorContext context) {
            super(context);
        }

        @Override
        protected CodeTypeElement create(NodeData node) {
            CodeTypeElement clazz = createClass(node, modifiers(PRIVATE, ABSTRACT, STATIC), genClassName(node), node.getNodeType(), false);

            for (NodeChildData child : node.getChildren()) {
                clazz.add(createChildField(child));

                if (child.getAccessElement() != null && child.getAccessElement().getModifiers().contains(Modifier.ABSTRACT)) {
                    ExecutableElement getter = (ExecutableElement) child.getAccessElement();
                    CodeExecutableElement method = CodeExecutableElement.clone(getContext().getEnvironment(), getter);
                    method.getModifiers().remove(Modifier.ABSTRACT);
                    method.createBuilder().startReturn().string("this.").string(child.getName()).end();
                    clazz.add(method);
                }
            }

            createConstructors(node, clazz);

            if (node.getExtensionElements() != null) {
                clazz.getEnclosedElements().addAll(node.getExtensionElements());
            }

            return clazz;
        }

        private void createConstructors(NodeData node, CodeTypeElement clazz) {
            List<ExecutableElement> signatureConstructors = new ArrayList<>();
            ExecutableElement copyConstructor = null;
            List<? extends ExecutableElement> constructors = ElementFilter.constructorsIn(Utils.fromTypeMirror(node.getNodeType()).getEnclosedElements());
            for (ExecutableElement constructor : constructors) {
                if (constructor.getModifiers().contains(Modifier.PRIVATE) || constructor.getParameters().isEmpty()) {
                    continue;
                }

                if (isCopyConstructor(constructor)) {
                    assert copyConstructor == null;
                    copyConstructor = createConstructor(clazz, constructor, true);
                } else {
                    signatureConstructors.add(createConstructor(clazz, constructor, false));
                }
            }

            if (copyConstructor == null && node.needsRewrites(getContext())) {
                clazz.add(createConstructor(clazz, null, true));
            } else if (copyConstructor != null) {
                clazz.add(copyConstructor);
            }

            if (signatureConstructors.isEmpty() && !node.getChildren().isEmpty()) {
                clazz.add(createConstructor(clazz, null, false));
            } else {
                clazz.getEnclosedElements().addAll(signatureConstructors);
            }
        }

        private CodeExecutableElement createConstructor(CodeTypeElement type, ExecutableElement superConstructor, boolean copyConstructor) {
            CodeExecutableElement method = new CodeExecutableElement(null, type.getSimpleName().toString());
            CodeTreeBuilder builder = method.createBuilder();
            if (!copyConstructor) {
                if (superConstructor != null) {
                    for (VariableElement param : superConstructor.getParameters()) {
                        method.getParameters().add(CodeVariableElement.clone(param));
                    }
                }
                for (NodeChildData child : getModel().getChildren()) {
                    method.getParameters().add(new CodeVariableElement(child.getNodeType(), child.getName()));
                }
            } else {
                if (!(superConstructor == null && getModel().getChildren().isEmpty())) {
                    method.getParameters().add(new CodeVariableElement(type.asType(), "copy"));
                }
            }

            if (superConstructor != null) {
                builder.startStatement();
                builder.startSuperCall();
                if (copyConstructor) {
                    builder.string("copy");
                } else {
                    for (VariableElement param : superConstructor.getParameters()) {
                        builder.string(param.getSimpleName().toString());
                    }
                }
                builder.end();
                builder.end();
            }

            for (NodeChildData child : getModel().getChildren()) {

                builder.startStatement();
                builder.string("this.").string(child.getName()).string(" = ");

                if (child.getCardinality() == Cardinality.MANY) {
                    builder.startCall("adoptChildren");
                } else {
                    builder.startCall("adoptChild");
                }

                builder.startGroup();
                if (copyConstructor) {
                    builder.string("copy.");
                }
                builder.string(child.getName());
                builder.end();

                builder.end();
                builder.end();

            }

            return method;
        }

        private CodeVariableElement createChildField(NodeChildData child) {
            DeclaredType annotationType;
            if (child.getCardinality() == Cardinality.MANY) {
                annotationType = getContext().getTruffleTypes().getChildrenAnnotation();
            } else {
                annotationType = getContext().getTruffleTypes().getChildAnnotation();
            }

            CodeVariableElement var = new CodeVariableElement(child.getNodeType(), child.getName());
            var.getModifiers().add(Modifier.PROTECTED);
            var.getAnnotationMirrors().add(new CodeAnnotationMirror(annotationType));
            return var;
        }
    }

    private class NodeFactoryFactory extends ClassElementFactory<NodeData> {

        private final Map<NodeData, List<TypeElement>> childTypes;

        private CodeTypeElement generatedNode;

        public NodeFactoryFactory(ProcessorContext context, Map<NodeData, List<TypeElement>> childElements) {
            super(context);
            this.childTypes = childElements;
        }

        @Override
        protected CodeTypeElement create(NodeData node) {
            Modifier visibility = Utils.getVisibility(node.getTemplateType().getModifiers());
            CodeTypeElement clazz = createClass(node, modifiers(), factoryClassName(node), null, false);
            if (visibility != null) {
                clazz.getModifiers().add(visibility);
            }
            clazz.getModifiers().add(Modifier.FINAL);
            clazz.add(createConstructorUsingFields(modifiers(PRIVATE), clazz));
            return clazz;
        }

        @Override
        protected void createChildren(NodeData node) {
            CodeTypeElement clazz = getElement();

            Modifier createVisibility = Utils.getVisibility(clazz.getModifiers());

            if (node.needsFactory()) {
                NodeGenFactory factory = new NodeGenFactory(context);
                add(factory, node);
                generatedNode = factory.getElement();

                createFactoryMethods(node, clazz, createVisibility);

                if (node.getSpecializations().size() > 1) {
                    clazz.add(createCreateSpecializedMethod(node, createVisibility));
                }

                if (node.needsRewrites(context)) {
                    clazz.add(createSpecializeMethod(node));
                }

                if (node.getGenericSpecialization() != null) {
                    List<CodeExecutableElement> genericMethods = createGeneratedGenericMethod(node);
                    for (CodeExecutableElement method : genericMethods) {
                        clazz.add(method);
                    }
                }

                for (SpecializationData specialization : node.getSpecializations()) {
                    add(new SpecializedNodeFactory(context, generatedNode), specialization);
                }

                TypeMirror nodeFactory = Utils.getDeclaredType(Utils.fromTypeMirror(getContext().getType(NodeFactory.class)), node.getNodeType());
                clazz.getImplements().add(nodeFactory);
                clazz.add(createCreateNodeMethod(node));
                clazz.add(createCreateNodeSpecializedMethod(node));
                clazz.add(createGetNodeClassMethod(node));
                clazz.add(createGetNodeSignaturesMethod());
                clazz.add(createGetChildrenSignatureMethod(node));
                clazz.add(createGetInstanceMethod(node, createVisibility));
                clazz.add(createInstanceConstant(node, clazz.asType()));
            }

            for (NodeData childNode : childTypes.keySet()) {
                if (childNode.getTemplateType().getModifiers().contains(Modifier.PRIVATE)) {
                    continue;
                }

                for (TypeElement type : childTypes.get(childNode)) {
                    Set<Modifier> typeModifiers = ((CodeTypeElement) type).getModifiers();
                    Modifier visibility = Utils.getVisibility(type.getModifiers());
                    typeModifiers.clear();
                    if (visibility != null) {
                        typeModifiers.add(visibility);
                    }

                    typeModifiers.add(Modifier.STATIC);
                    typeModifiers.add(Modifier.FINAL);
                    clazz.add(type);
                }
            }

            List<NodeData> children = node.getNodeDeclaringChildren();
            if (node.getParent() == null && children.size() > 0) {
                clazz.add(createGetFactories(node));
            }

        }

        private CodeExecutableElement createGetNodeClassMethod(NodeData node) {
            TypeMirror returnType = Utils.getDeclaredType(Utils.fromTypeMirror(getContext().getType(Class.class)), node.getNodeType());
            CodeExecutableElement method = new CodeExecutableElement(modifiers(PUBLIC), returnType, "getNodeClass");
            CodeTreeBuilder builder = method.createBuilder();
            builder.startReturn().typeLiteral(node.getNodeType()).end();
            return method;
        }

        private CodeExecutableElement createGetNodeSignaturesMethod() {
            TypeElement listType = Utils.fromTypeMirror(getContext().getType(List.class));
            TypeMirror classType = getContext().getType(Class.class);
            TypeMirror returnType = Utils.getDeclaredType(listType, Utils.getDeclaredType(listType, classType));
            CodeExecutableElement method = new CodeExecutableElement(modifiers(PUBLIC), returnType, "getNodeSignatures");
            CodeTreeBuilder builder = method.createBuilder();
            builder.startReturn();
            builder.startStaticCall(getContext().getType(Arrays.class), "asList");
            List<ExecutableElement> constructors = findUserConstructors(generatedNode.asType());
            for (ExecutableElement constructor : constructors) {
                builder.tree(createAsList(builder, Utils.asTypeMirrors(constructor.getParameters()), classType));
            }
            builder.end();
            builder.end();
            return method;
        }

        private CodeExecutableElement createGetChildrenSignatureMethod(NodeData node) {
            Types types = getContext().getEnvironment().getTypeUtils();
            TypeElement listType = Utils.fromTypeMirror(getContext().getType(List.class));
            TypeMirror classType = getContext().getType(Class.class);
            TypeMirror nodeType = getContext().getTruffleTypes().getNode();
            TypeMirror wildcardNodeType = types.getWildcardType(nodeType, null);
            classType = Utils.getDeclaredType(Utils.fromTypeMirror(classType), wildcardNodeType);
            TypeMirror returnType = Utils.getDeclaredType(listType, classType);

            CodeExecutableElement method = new CodeExecutableElement(modifiers(PUBLIC), returnType, "getExecutionSignature");
            CodeTreeBuilder builder = method.createBuilder();

            List<TypeMirror> signatureTypes = new ArrayList<>();
            assert !node.getSpecializations().isEmpty();
            SpecializationData data = node.getSpecializations().get(0);
            for (ActualParameter parameter : data.getParameters()) {
                ParameterSpec spec = parameter.getSpecification();
                NodeChildData field = node.findChild(spec.getName());
                if (field == null) {
                    continue;
                }

                TypeMirror type;
                if (field.getCardinality() == Cardinality.MANY && field.getNodeType().getKind() == TypeKind.ARRAY) {
                    type = ((ArrayType) field.getNodeType()).getComponentType();
                } else {
                    type = field.getNodeType();
                }

                signatureTypes.add(type);
            }

            builder.startReturn().tree(createAsList(builder, signatureTypes, classType)).end();
            return method;
        }

        private CodeTree createAsList(CodeTreeBuilder parent, List<TypeMirror> types, TypeMirror elementClass) {
            CodeTreeBuilder builder = parent.create();
            builder.startGroup();
            builder.type(getContext().getType(Arrays.class));
            builder.string(".<").type(elementClass).string(">");
            builder.startCall("asList");
            for (TypeMirror typeMirror : types) {
                builder.typeLiteral(typeMirror);
            }
            builder.end().end();
            return builder.getRoot();
        }

        private CodeExecutableElement createCreateNodeMethod(NodeData node) {
            CodeExecutableElement method = new CodeExecutableElement(modifiers(PUBLIC), node.getNodeType(), "createNode");
            CodeVariableElement arguments = new CodeVariableElement(getContext().getType(Object.class), "arguments");
            method.setVarArgs(true);
            method.addParameter(arguments);

            CodeTreeBuilder builder = method.createBuilder();
            List<ExecutableElement> signatures = findUserConstructors(generatedNode.asType());
            boolean ifStarted = false;

            for (ExecutableElement element : signatures) {
                ifStarted = builder.startIf(ifStarted);
                builder.string("arguments.length == " + element.getParameters().size());

                int index = 0;
                for (VariableElement param : element.getParameters()) {
                    builder.string(" && ");
                    if (!param.asType().getKind().isPrimitive()) {
                        builder.string("(arguments[" + index + "] == null || ");
                    }
                    builder.string("arguments[" + index + "] instanceof ");
                    builder.type(Utils.boxType(getContext(), param.asType()));
                    if (!param.asType().getKind().isPrimitive()) {
                        builder.string(")");
                    }
                    index++;
                }
                builder.end();
                builder.startBlock();

                builder.startReturn().startCall("create");
                index = 0;
                for (VariableElement param : element.getParameters()) {
                    builder.startGroup();
                    builder.string("(").type(param.asType()).string(") ");
                    builder.string("arguments[").string(String.valueOf(index)).string("]");
                    builder.end();
                    index++;
                }
                builder.end().end();

                builder.end(); // block
            }

            builder.startElseBlock();
            builder.startThrow().startNew(getContext().getType(IllegalArgumentException.class));
            builder.doubleQuote("Invalid create signature.");
            builder.end().end();
            builder.end(); // else block
            return method;
        }

        private CodeExecutableElement createCreateNodeSpecializedMethod(NodeData node) {
            CodeExecutableElement method = new CodeExecutableElement(modifiers(PUBLIC), node.getNodeType(), "createNodeSpecialized");
            CodeVariableElement nodeParam = new CodeVariableElement(node.getNodeType(), "thisNode");
            CodeVariableElement arguments = new CodeVariableElement(getContext().getType(Class.class), "types");
            method.addParameter(nodeParam);
            method.addParameter(arguments);
            method.setVarArgs(true);

            CodeTreeBuilder builder = method.createBuilder();
            if (!node.needsRewrites(getContext())) {
                builder.startThrow().startNew(getContext().getType(UnsupportedOperationException.class)).end().end();
            } else {
                builder.startIf();
                builder.string("types.length == 1");
                builder.end();
                builder.startBlock();

                builder.startReturn().startCall("createSpecialized");
                builder.string("thisNode");
                builder.string("types[0]");
                builder.end().end();

                builder.end();
                builder.startElseBlock();
                builder.startThrow().startNew(getContext().getType(IllegalArgumentException.class));
                builder.doubleQuote("Invalid createSpecialized signature.");
                builder.end().end();
                builder.end();
            }

            return method;
        }

        private ExecutableElement createGetInstanceMethod(NodeData node, Modifier visibility) {
            TypeElement nodeFactoryType = Utils.fromTypeMirror(getContext().getType(NodeFactory.class));
            TypeMirror returnType = Utils.getDeclaredType(nodeFactoryType, node.getNodeType());

            CodeExecutableElement method = new CodeExecutableElement(modifiers(), returnType, "getInstance");
            if (visibility != null) {
                method.getModifiers().add(visibility);
            }
            method.getModifiers().add(Modifier.STATIC);

            String varName = instanceVarName(node);

            CodeTreeBuilder builder = method.createBuilder();
            builder.startIf();
            builder.string(varName).string(" == null");
            builder.end().startBlock();

            builder.startStatement();
            builder.string(varName);
            builder.string(" = ");
            builder.startNew(factoryClassName(node)).end();
            builder.end();

            builder.end();
            builder.startReturn().string(varName).end();
            return method;
        }

        private String instanceVarName(NodeData node) {
            if (node.getParent() != null) {
                return Utils.firstLetterLowerCase(factoryClassName(node)) + "Instance";
            } else {
                return "instance";
            }
        }

        private CodeVariableElement createInstanceConstant(NodeData node, TypeMirror factoryType) {
            String varName = instanceVarName(node);
            CodeVariableElement var = new CodeVariableElement(modifiers(), factoryType, varName);
            var.getModifiers().add(Modifier.PRIVATE);
            var.getModifiers().add(Modifier.STATIC);
            return var;
        }

        private ExecutableElement createGetFactories(NodeData node) {
            List<NodeData> children = node.getNodeDeclaringChildren();
            if (node.needsFactory()) {
                children.add(node);
            }

            List<TypeMirror> nodeTypesList = new ArrayList<>();
            TypeMirror prev = null;
            boolean allSame = true;
            for (NodeData child : children) {
                nodeTypesList.add(child.getNodeType());
                if (prev != null && !Utils.typeEquals(child.getNodeType(), prev)) {
                    allSame = false;
                }
                prev = child.getNodeType();
            }
            TypeMirror commonNodeSuperType = Utils.getCommonSuperType(getContext(), nodeTypesList.toArray(new TypeMirror[nodeTypesList.size()]));

            Types types = getContext().getEnvironment().getTypeUtils();
            TypeMirror factoryType = getContext().getType(NodeFactory.class);
            TypeMirror baseType;
            if (allSame) {
                baseType = Utils.getDeclaredType(Utils.fromTypeMirror(factoryType), commonNodeSuperType);
            } else {
                baseType = Utils.getDeclaredType(Utils.fromTypeMirror(factoryType), types.getWildcardType(commonNodeSuperType, null));
            }
            TypeMirror listType = Utils.getDeclaredType(Utils.fromTypeMirror(getContext().getType(List.class)), baseType);

            CodeExecutableElement method = new CodeExecutableElement(modifiers(PUBLIC, STATIC), listType, "getFactories");

            CodeTreeBuilder builder = method.createBuilder();
            builder.startReturn();
            builder.startStaticCall(getContext().getType(Arrays.class), "asList");

            for (NodeData child : children) {
                builder.startGroup();
                NodeData childNode = child;
                List<NodeData> factories = new ArrayList<>();
                while (childNode.getParent() != null) {
                    factories.add(childNode);
                    childNode = childNode.getParent();
                }
                Collections.reverse(factories);
                for (NodeData nodeData : factories) {
                    builder.string(factoryClassName(nodeData)).string(".");
                }
                builder.string("getInstance()");
                builder.end();
            }
            builder.end();
            builder.end();
            return method;
        }

        private void createFactoryMethods(NodeData node, CodeTypeElement clazz, Modifier createVisibility) {
            List<ExecutableElement> constructors = findUserConstructors(generatedNode.asType());
            for (ExecutableElement constructor : constructors) {
                clazz.add(createCreateMethod(node, createVisibility, constructor));
            }
        }

        private CodeExecutableElement createCreateMethod(NodeData node, Modifier visibility, ExecutableElement constructor) {
            CodeExecutableElement method = CodeExecutableElement.clone(getContext().getEnvironment(), constructor);
            method.setSimpleName(CodeNames.of("create"));
            method.getModifiers().clear();
            if (visibility != null) {
                method.getModifiers().add(visibility);
            }
            method.getModifiers().add(Modifier.STATIC);
            method.setReturnType(node.getNodeType());

            CodeTreeBuilder body = method.createBuilder();
            body.startReturn();
            if (node.getSpecializations().isEmpty()) {
                body.null_();
            } else {
                body.startNew(nodeSpecializationClassName(node.getSpecializations().get(0)));
                for (VariableElement var : method.getParameters()) {
                    body.string(var.getSimpleName().toString());
                }
                body.end();
            }
            body.end();
            return method;
        }

        private CodeExecutableElement createCreateSpecializedMethod(NodeData node, Modifier visibility) {
            CodeExecutableElement method = new CodeExecutableElement(modifiers(), node.getNodeType(), "createSpecialized");
            if (visibility != null) {
                method.getModifiers().add(visibility);
            }
            method.getModifiers().add(Modifier.STATIC);

            method.addParameter(new CodeVariableElement(node.getNodeType(), THIS_NODE_LOCAL_VAR_NAME));
            method.addParameter(new CodeVariableElement(getContext().getType(Class.class), "specializationClass"));

            CodeTreeBuilder body = method.createBuilder();

            body.startStatement();
            body.type(generatedNode.asType()).string(" ").string(THIS_NODE_LOCAL_VAR_NAME + "Cast");
            body.string(" = ").string("(").type(generatedNode.asType()).string(") ").string(THIS_NODE_LOCAL_VAR_NAME);
            body.end();

            boolean first = true;
            for (TypeData type : node.getTypeSystem().getTypes()) {
                SpecializationData specialization = node.findUniqueSpecialization(type);
                if (specialization != null && !type.isGeneric()) {
                    if (first) {
                        body.startIf();
                        first = false;
                    } else {
                        body.startElseIf();
                    }
                    body.string("specializationClass == ").type(type.getBoxedType()).string(".class").end().startBlock();
                    body.startReturn().startNew(nodeSpecializationClassName(specialization));
                    body.string(THIS_NODE_LOCAL_VAR_NAME + "Cast");
                    body.end().end(); // new, return

                    body.end(); // if
                }
            }
            body.startReturn().startNew(nodeSpecializationClassName(node.getGenericSpecialization()));
            body.string(THIS_NODE_LOCAL_VAR_NAME + "Cast");
            body.end().end();
            return method;
        }

        private CodeExecutableElement createSpecializeMethod(NodeData node) {
            CodeExecutableElement method = new CodeExecutableElement(modifiers(PRIVATE, STATIC), node.getNodeType(), "specialize");
            method.addParameter(new CodeVariableElement(generatedNode.asType(), THIS_NODE_LOCAL_VAR_NAME));
            method.addParameter(new CodeVariableElement(getContext().getType(Class.class), "minimumState"));
            addInternalValueParameters(method, node.getGenericSpecialization(), true);

            CodeTreeBuilder body = method.createBuilder();
            body.startStatement().string("boolean allowed = (minimumState == ").string(nodeSpecializationClassName(node.getSpecializations().get(0))).string(".class)").end();

            for (int i = 1; i < node.getSpecializations().size(); i++) {
                SpecializationData specialization = node.getSpecializations().get(i);
                body.startStatement().string("allowed = allowed || (minimumState == ").string(nodeSpecializationClassName(specialization)).string(".class)").end();

                CodeTreeBuilder guarded = new CodeTreeBuilder(body);
                guarded.startReturn().startNew(nodeSpecializationClassName(specialization));
                guarded.string(THIS_NODE_LOCAL_VAR_NAME);
                guarded.end().end();

                body.tree(createGuardAndCast(body, "allowed", node.getGenericSpecialization(), specialization, false, guarded.getRoot(), null));
            }
            body.startThrow().startNew(getContext().getType(IllegalArgumentException.class)).end().end();

            return method;
        }

        private List<CodeExecutableElement> createGeneratedGenericMethod(NodeData node) {
            TypeMirror genericReturnType = node.getGenericSpecialization().getReturnType().getType();
            if (node.needsRewrites(context)) {
                List<CodeExecutableElement> methods = new ArrayList<>();

                List<SpecializationData> specializations = node.getSpecializations();
                SpecializationData prev = null;
                for (int i = 0; i < specializations.size(); i++) {
                    SpecializationData current = specializations.get(i);
                    SpecializationData next = i + 1 < specializations.size() ? specializations.get(i + 1) : null;
                    if (prev == null || current.isUninitialized()) {
                        prev = current;
                        continue;
                    } else {
                        String methodName = generatedGenericMethodName(current);
                        CodeExecutableElement method = new CodeExecutableElement(modifiers(PRIVATE, STATIC), genericReturnType, methodName);
                        method.addParameter(new CodeVariableElement(node.getNodeType(), THIS_NODE_LOCAL_VAR_NAME));
                        addInternalValueParameters(method, node.getGenericSpecialization(), true);

                        emitGeneratedGenericSpecialization(method.createBuilder(), current, next);

                        methods.add(method);
                    }
                    prev = current;
                }

                return methods;
            } else {
                CodeExecutableElement method = new CodeExecutableElement(modifiers(PRIVATE, STATIC), genericReturnType, generatedGenericMethodName(null));
                method.addParameter(new CodeVariableElement(node.getNodeType(), THIS_NODE_LOCAL_VAR_NAME));
                addInternalValueParameters(method, node.getGenericSpecialization(), true);
                emitInvokeDoMethod(method.createBuilder(), node.getGenericSpecialization(), 0);
                return Arrays.asList(method);
            }
        }

        private void emitGeneratedGenericSpecialization(CodeTreeBuilder builder, SpecializationData current, SpecializationData next) {
            CodeTreeBuilder invokeMethodBuilder = new CodeTreeBuilder(builder);
            emitInvokeDoMethod(invokeMethodBuilder, current, 0);
            CodeTree invokeMethod = invokeMethodBuilder.getRoot();

            if (next != null) {
                CodeTreeBuilder nextBuilder = builder.create();

                nextBuilder.startReturn().startCall(generatedGenericMethodName(next));
                nextBuilder.string(THIS_NODE_LOCAL_VAR_NAME);
                addInternalValueParameterNames(nextBuilder, next, next, null, true, true);
                nextBuilder.end().end();

                invokeMethod = createGuardAndCast(builder, null, current.getNode().getGenericSpecialization(), current, true, invokeMethod, nextBuilder.getRoot());
            }

            builder.tree(invokeMethod);

            if (next != null) {
                builder.end();
            }
        }

        private void emitInvokeDoMethod(CodeTreeBuilder builder, SpecializationData specialization, int level) {
            if (!specialization.getExceptions().isEmpty()) {
                builder.startTryBlock();
            }

            if (specialization.getMethod() == null) {
                emitEncounteredSynthetic(builder);
            } else {
                builder.startReturn();
                builder.tree(createTemplateMethodCall(builder, specialization.getNode().getGenericSpecialization(), specialization, null));
                builder.end(); // return
            }

            if (!specialization.getExceptions().isEmpty()) {
                for (SpecializationThrowsData exception : specialization.getExceptions()) {
                    builder.end().startCatchBlock(exception.getJavaClass(), "ex" + level);

                    builder.startReturn().startCall(generatedGenericMethodName(exception.getTransitionTo()));
                    builder.string(THIS_NODE_LOCAL_VAR_NAME);
                    addInternalValueParameterNames(builder, exception.getTransitionTo(), exception.getTransitionTo(), null, true, true);
                    builder.end().end();
                }
                builder.end();
            }
        }

    }

    private class SpecializedNodeFactory extends ClassElementFactory<SpecializationData> {

        private final CodeTypeElement nodeGen;

        public SpecializedNodeFactory(ProcessorContext context, CodeTypeElement nodeGen) {
            super(context);
            this.nodeGen = nodeGen;
        }

        @Override
        public CodeTypeElement create(SpecializationData specialization) {
            NodeData node = specialization.getNode();
            TypeMirror baseType = node.getNodeType();
            if (nodeGen != null) {
                baseType = nodeGen.asType();
            }
            CodeTypeElement clazz = createClass(node, modifiers(PRIVATE, STATIC, FINAL), nodeSpecializationClassName(specialization), baseType, false);
            return clazz;
        }

        @Override
        protected void createChildren(SpecializationData specialization) {
            CodeTypeElement clazz = getElement();
            NodeData node = specialization.getNode();

            TypeElement superTypeElement = Utils.fromTypeMirror(clazz.getSuperclass());
            for (ExecutableElement constructor : ElementFilter.constructorsIn(superTypeElement.getEnclosedElements())) {
                ExecutableElement superConstructor = createSuperConstructor(clazz, constructor);
                if (superConstructor != null) {
                    clazz.add(superConstructor);
                }
            }

            for (ExecutableTypeData execType : node.getExecutableTypes()) {
                if (execType.isFinal()) {
                    continue;
                }
                CodeExecutableElement executeMethod = createExecutableTypeOverride(execType);
                clazz.add(executeMethod);
                CodeTreeBuilder builder = executeMethod.createBuilder();
                CodeTree result = createExecuteBody(builder, specialization, execType);
                if (result != null) {
                    builder.tree(result);
                } else {
                    clazz.remove(executeMethod);
                }
            }

            if (node.needsRewrites(getContext()) && !specialization.isGeneric() && !specialization.isUninitialized()) {
                buildSpecializeAndExecute(clazz, specialization);
            }
        }

        private CodeTree createExecuteBody(CodeTreeBuilder parent, SpecializationData specialization, ExecutableTypeData execType) {
            TypeData primaryType = specialization.getReturnType().getTypeSystemType();

            CodeTreeBuilder builder = new CodeTreeBuilder(parent);

            ExecutableTypeData foundEvaluatedPrimaryType = findFunctionalExecutableType(specialization, execType.getEvaluatedCount());

            if (execType == foundEvaluatedPrimaryType || foundEvaluatedPrimaryType == null) {
                builder.tree(createFunctionalExecute(builder, specialization, execType));
            } else if (needsCastingExecuteMethod(execType, primaryType)) {
                builder.tree(createCastingExecute(builder, specialization, execType, foundEvaluatedPrimaryType));
            } else {
                return null;
            }

            return builder.getRoot();
        }

        private CodeExecutableElement createExecutableTypeOverride(ExecutableTypeData execType) {
            CodeExecutableElement method = CodeExecutableElement.clone(getContext().getEnvironment(), execType.getMethod());

            int i = 0;
            for (VariableElement param : method.getParameters()) {
                CodeVariableElement var = CodeVariableElement.clone(param);
                var.setName(valueName(execType.getParameters().get(i)));
                method.getParameters().set(i, var);
                i++;
            }

            method.getAnnotationMirrors().clear();
            method.getModifiers().remove(Modifier.ABSTRACT);
            return method;
        }

        private boolean needsCastingExecuteMethod(ExecutableTypeData execType, TypeData primaryType) {
            if (execType.isAbstract()) {
                return true;
            }
            if (Utils.isPrimitiveOrVoid(primaryType.getPrimitiveType()) && Utils.isPrimitiveOrVoid(execType.getType().getPrimitiveType())) {
                return true;
            }
            if (execType.getType().isGeneric()) {
                return true;
            }
            return false;
        }

        private ExecutableTypeData findFunctionalExecutableType(SpecializationData specialization, int evaluatedCount) {
            TypeData primaryType = specialization.getReturnType().getTypeSystemType();
            List<ExecutableTypeData> otherTypes = specialization.getNode().getExecutableTypes(evaluatedCount);

            List<ExecutableTypeData> filteredTypes = new ArrayList<>();
            for (ExecutableTypeData compareType : otherTypes) {
                if (!Utils.typeEquals(compareType.getType().getPrimitiveType(), primaryType.getPrimitiveType())) {
                    continue;
                }
                filteredTypes.add(compareType);
            }

            for (ExecutableTypeData compareType : filteredTypes) {
                if (compareType.startsWithSignature(specialization)) {
                    return compareType;
                }
            }

            for (ExecutableTypeData compareType : otherTypes) {
                if (compareType.startsWithSignature(specialization)) {
                    return compareType;
                }
            }

            return null;
        }

        private CodeTree createCastingExecute(CodeTreeBuilder parent, SpecializationData specialization, ExecutableTypeData executable, ExecutableTypeData castExecutable) {
            TypeData type = executable.getType();
            CodeTreeBuilder builder = new CodeTreeBuilder(parent);
            NodeData node = specialization.getNode();

            ExecutableTypeData castedType = node.findExecutableType(type);
            TypeData primaryType = castExecutable.getType();

            boolean needsTry = castExecutable.hasUnexpectedValue(getContext());
            boolean returnVoid = type.isVoid();

            List<ActualParameter> executeParameters = new ArrayList<>();
            for (ActualParameter sourceParameter : executable.getParameters()) {
                NodeChildData field = specialization.getNode().findChild(sourceParameter.getSpecification().getName());
                if (field == null) {
                    continue;
                }

                ActualParameter targetParameter = castExecutable.findParameter(sourceParameter.getLocalName());
                if (targetParameter != null) {
                    TypeData sourceType = sourceParameter.getTypeSystemType();
                    TypeData targetType = targetParameter.getTypeSystemType();
                    if (sourceType.needsCastTo(targetType)) {
                        executeParameters.add(targetParameter);
                    }
                }
            }

            builder.tree(createExecuteChildren(builder, executable, specialization, executeParameters, null, true));

            CodeTree primaryExecuteCall = createTemplateMethodCall(builder, executable, castExecutable, null);
            if (needsTry) {
                if (!returnVoid) {
                    builder.declaration(primaryType.getPrimitiveType(), "value");
                }
                builder.startTryBlock();

                if (returnVoid) {
                    builder.statement(primaryExecuteCall);
                } else {
                    builder.startStatement();
                    builder.string("value = ");
                    builder.tree(primaryExecuteCall);
                    builder.end();
                }

                builder.end().startCatchBlock(getUnexpectedValueException(), "ex");
                if (returnVoid) {
                    builder.string("// ignore").newLine();
                } else {
                    builder.startReturn();
                    builder.tree(createExpectType(node, castedType, CodeTreeBuilder.singleString("ex.getResult()")));
                    builder.end();
                }
                builder.end();

                if (!returnVoid) {
                    builder.startReturn();
                    builder.tree(createExpectType(node, castedType, CodeTreeBuilder.singleString("value")));
                    builder.end();
                }
            } else {
                if (returnVoid) {
                    builder.statement(primaryExecuteCall);
                } else {
                    builder.startReturn();
                    builder.tree(createExpectType(node, castedType, primaryExecuteCall));
                    builder.end();
                }
            }

            return builder.getRoot();
        }

        private CodeTree createExpectType(NodeData node, ExecutableTypeData castedType, CodeTree value) {
            if (castedType == null) {
                return value;
            } else if (castedType.getType().isVoid()) {
                return value;
            } else if (castedType.getType().isGeneric()) {
                return value;
            }

            CodeTreeBuilder builder = CodeTreeBuilder.createBuilder();
            String targetMethodName;
            if (castedType.hasUnexpectedValue(getContext())) {
                targetMethodName = TypeSystemCodeGenerator.expectTypeMethodName(castedType.getType());
            } else {
                targetMethodName = TypeSystemCodeGenerator.asTypeMethodName(castedType.getType());
            }
            startCallTypeSystemMethod(getContext(), builder, node, targetMethodName);

            builder.tree(value);
            builder.end().end();
            return builder.getRoot();
        }

        private CodeTree createFunctionalExecute(CodeTreeBuilder parent, SpecializationData specialization, ExecutableTypeData executable) {
            CodeTreeBuilder builder = new CodeTreeBuilder(parent);
            if (specialization.isUninitialized()) {
                builder.tree(createDeoptimize(builder));
            }

            builder.tree(createExecuteChildren(builder, executable, specialization, specialization.getParameters(), null, false));

            CodeTree executeNode;
            if (specialization.isUninitialized()) {
                builder.tree(createSpecializeCall(builder, executable, specialization));
            }
            executeNode = createExecute(builder, executable, specialization);

            SpecializationData next = specialization.findNextSpecialization();
            CodeTree returnSpecialized = null;
            if (next != null) {
                returnSpecialized = createReturnSpecializeAndExecute(builder, executable, next, null);
            }
            builder.tree(createGuardAndCast(builder, null, specialization, specialization, true, executeNode, returnSpecialized));

            return builder.getRoot();
        }

        private CodeTree createDeoptimize(CodeTreeBuilder parent) {
            CodeTreeBuilder builder = new CodeTreeBuilder(parent);
            builder.startStatement();
            builder.startStaticCall(getContext().getTruffleTypes().getTruffleIntrinsics(), "deoptimize").end();
            builder.end();
            return builder.getRoot();
        }

        private CodeTree createSpecializeCall(CodeTreeBuilder parent, ExecutableTypeData executable, SpecializationData specialization) {
            NodeData node = specialization.getNode();

            CodeTreeBuilder builder = new CodeTreeBuilder(parent);
            emitSpecializationListeners(builder, node);

            builder.startStatement();
            builder.startCall("replace");
            if (node.needsRewrites(getContext())) {
                builder.startCall(factoryClassName(node), "specialize");
                builder.string("this");
                builder.typeLiteral(builder.findMethod().getEnclosingElement().asType());
                addInternalValueParameterNames(builder, executable, specialization, null, true, true);
                builder.end(); // call replace, call specialize
            } else {
                builder.startCall(factoryClassName(node), "createSpecialized").string("this").string("null").end();
            }
            builder.end().end();
            return builder.getRoot();
        }

        private CodeTree createExecute(CodeTreeBuilder parent, ExecutableTypeData executable, SpecializationData specialization) {
            NodeData node = specialization.getNode();
            CodeTreeBuilder builder = new CodeTreeBuilder(parent);
            if (!specialization.getExceptions().isEmpty()) {
                builder.startTryBlock();
            }

            CodeTreeBuilder returnBuilder = new CodeTreeBuilder(parent);
            if (specialization.isUninitialized()) {
                String genericMethodName = generatedGenericMethodName(null);
                returnBuilder.startCall(factoryClassName(node), genericMethodName);
                returnBuilder.string("this");
                addInternalValueParameterNames(returnBuilder, executable, specialization, null, true, true);
                returnBuilder.end();
            } else if (specialization.getMethod() == null && !node.needsRewrites(context)) {
                emitEncounteredSynthetic(builder);
            } else if (specialization.isGeneric()) {
                String genericMethodName;
                if (!specialization.isUseSpecializationsForGeneric()) {
                    genericMethodName = generatedGenericMethodName(specialization);
                } else {
                    genericMethodName = generatedGenericMethodName(null);
                }

                returnBuilder.startCall(factoryClassName(node), genericMethodName);
                returnBuilder.string("this");
                addInternalValueParameterNames(returnBuilder, executable, specialization, null, true, true);
                returnBuilder.end();
            } else {
                returnBuilder.tree(createTemplateMethodCall(returnBuilder, executable, specialization, null));
            }

            if (!returnBuilder.isEmpty()) {
                builder.startReturn();

                TypeData targetType = node.getTypeSystem().findTypeData(builder.findMethod().getReturnType());
                TypeData sourceType = specialization.getReturnType().getTypeSystemType();

                if (targetType == null || sourceType == null) {
                    builder.tree(returnBuilder.getRoot());
                } else if (sourceType.needsCastTo(targetType)) {
                    builder.tree(createCallTypeSystemMethod(context, parent, node, TypeSystemCodeGenerator.expectTypeMethodName(targetType), returnBuilder.getRoot()));
                } else {
                    builder.tree(returnBuilder.getRoot());
                }
                builder.end();
            }

            if (!specialization.getExceptions().isEmpty()) {
                for (SpecializationThrowsData exception : specialization.getExceptions()) {
                    builder.end().startCatchBlock(exception.getJavaClass(), "ex");
                    builder.tree(createReturnSpecializeAndExecute(parent, executable, exception.getTransitionTo(), null));
                }
                builder.end();
            }
            return builder.getRoot();
        }

        private CodeTree createExecuteChildren(CodeTreeBuilder parent, ExecutableTypeData sourceExecutable, SpecializationData specialization, List<ActualParameter> targetParameters,
                        ActualParameter unexpectedParameter, boolean cast) {
            NodeData sourceNode = specialization.getNode();

            CodeTreeBuilder builder = new CodeTreeBuilder(parent);

            for (ActualParameter targetParameter : targetParameters) {
                NodeChildData field = sourceNode.findChild(targetParameter.getSpecification().getName());
                if (field == null) {
                    continue;
                }
                TypeData targetType = targetParameter.getTypeSystemType();

                ExecutableTypeData targetExecutable = field.getNodeData().findExecutableType(targetType);

                ActualParameter sourceParameter = sourceExecutable.findParameter(targetParameter.getLocalName());

                String targetVariableName = null;
                CodeTree executionExpression = null;
                if (cast || sourceParameter != null) {
                    TypeData sourceType = sourceParameter.getTypeSystemType();
                    if (!sourceType.needsCastTo(targetType)) {
                        if (field.isShortCircuit() && sourceParameter != null) {
                            builder.tree(createShortCircuitValue(builder, sourceExecutable, specialization, field, targetParameter.getPreviousParameter(), unexpectedParameter));
                        }
                        continue;
                    }
                    executionExpression = createExpectType(sourceNode, targetExecutable, CodeTreeBuilder.singleString(valueName(targetParameter)));
                    targetVariableName = castValueName(targetParameter);
                } else if (sourceParameter == null) {
                    targetVariableName = valueName(targetParameter);
                    executionExpression = createExecuteChildExpression(builder, field, targetParameter);
                }

                CodeTreeVariable executionVar = new CodeTreeVariable();
                CodeTree shortCircuitTree = createShortCircuitTree(builder, executionVar, targetVariableName, sourceExecutable, specialization, targetParameter, unexpectedParameter);
                CodeTree unexpectedTree = createCatchUnexpectedTree(builder, executionExpression, targetVariableName, specialization, sourceExecutable, targetExecutable, targetParameter,
                                shortCircuitTree != executionVar);

                executionVar.set(unexpectedTree);
                builder.tree(shortCircuitTree);
            }
            return builder.getRoot();
        }

        private void emitSpecializationListeners(CodeTreeBuilder builder, NodeData node) {
            for (TemplateMethod listener : node.getSpecializationListeners()) {
                builder.startStatement();
                builder.tree(createTemplateMethodCall(builder, listener, listener, null));
                builder.end(); // statement
            }
        }

        private CodeTree createCatchUnexpectedTree(CodeTreeBuilder parent, CodeTree body, String targetVariableName, SpecializationData specialization, ExecutableTypeData currentExecutable,
                        ExecutableTypeData targetExecutable, ActualParameter param, boolean shortCircuit) {
            CodeTreeBuilder builder = new CodeTreeBuilder(parent);
            boolean unexpected = targetExecutable.hasUnexpectedValue(getContext());
            builder.startStatement();

            if (!shortCircuit) {
                builder.type(param.getType()).string(" ").string(targetVariableName);
            }

            if (unexpected) {
                if (!shortCircuit) {
                    builder.end();
                }
                builder.startTryBlock();
                builder.startStatement();
                builder.string(targetVariableName);
            } else if (shortCircuit) {
                builder.startStatement();
                builder.string(targetVariableName);
            }
            builder.string(" = ");
            builder.tree(body);
            builder.end();

            if (unexpected) {
                builder.end().startCatchBlock(getUnexpectedValueException(), "ex");
                SpecializationData generic = specialization.getNode().getGenericSpecialization();
                ActualParameter genericParameter = generic.findParameter(param.getLocalName());

                List<ActualParameter> genericParameters = generic.getParametersAfter(genericParameter);
                builder.tree(createExecuteChildren(parent, currentExecutable, generic, genericParameters, genericParameter, false));
                builder.tree(createReturnSpecializeAndExecute(builder, currentExecutable, specialization.findNextSpecialization(), param));
                builder.end(); // catch block
            }

            return builder.getRoot();
        }

        private CodeTree createExecuteChildExpression(CodeTreeBuilder parent, NodeChildData targetField, ActualParameter sourceParameter) {
            TypeData type = sourceParameter.getTypeSystemType();
            ExecutableTypeData execType = targetField.getNodeData().findExecutableType(type);

            CodeTreeBuilder builder = new CodeTreeBuilder(parent);
            if (targetField != null) {
                Element accessElement = targetField.getAccessElement();
                if (accessElement == null) {
                    builder.string("this.").string(targetField.getName());
                } else if (accessElement.getKind() == ElementKind.METHOD) {
                    builder.startCall(accessElement.getSimpleName().toString()).end();
                } else if (accessElement.getKind() == ElementKind.FIELD) {
                    builder.string("this.").string(accessElement.getSimpleName().toString());
                } else {
                    throw new AssertionError();
                }
                if (sourceParameter.getSpecification().isIndexed()) {
                    builder.string("[" + sourceParameter.getIndex() + "]");
                }
                builder.string(".");
            }
            builder.startCall(execType.getMethodName());
            if (execType.getParameters().size() == 1) {
                builder.string("frameValue");
            }
            builder.end();
            return builder.getRoot();
        }

        private CodeTree createShortCircuitTree(CodeTreeBuilder parent, CodeTree body, String targetVariableName, ExecutableTypeData currentExecutable, SpecializationData specialization,
                        ActualParameter parameter, ActualParameter exceptionParam) {
            CodeTreeBuilder builder = new CodeTreeBuilder(parent);

            NodeChildData forField = specialization.getNode().findChild(parameter.getSpecification().getName());
            if (forField == null) {
                return body;
            }

            if (forField.getExecutionKind() != ExecutionKind.SHORT_CIRCUIT) {
                return body;
            }

            ActualParameter shortCircuitParam = specialization.getPreviousParam(parameter);

            builder.tree(createShortCircuitValue(builder, currentExecutable, specialization, forField, shortCircuitParam, exceptionParam));

            builder.declaration(parameter.getType(), targetVariableName, CodeTreeBuilder.createBuilder().defaultValue(parameter.getType()));
            builder.startIf().string(shortCircuitParam.getLocalName()).end();
            builder.startBlock();
            builder.tree(body);
            builder.end();

            return builder.getRoot();
        }

        private CodeTree createShortCircuitValue(CodeTreeBuilder parent, ExecutableTypeData currentExecutable, SpecializationData specialization, NodeChildData forField,
                        ActualParameter shortCircuitParam, ActualParameter exceptionParam) {
            CodeTreeBuilder builder = new CodeTreeBuilder(parent);
            int shortCircuitIndex = 0;
            for (NodeChildData field : specialization.getNode().getChildren()) {
                if (field.getExecutionKind() == ExecutionKind.SHORT_CIRCUIT) {
                    if (field == forField) {
                        break;
                    }
                    shortCircuitIndex++;
                }
            }

            builder.startStatement().type(shortCircuitParam.getType()).string(" ").string(valueName(shortCircuitParam)).string(" = ");
            ShortCircuitData shortCircuitData = specialization.getShortCircuits().get(shortCircuitIndex);
            builder.tree(createTemplateMethodCall(builder, currentExecutable, shortCircuitData, exceptionParam != null ? exceptionParam.getLocalName() : null));
            builder.end(); // statement

            return builder.getRoot();
        }

        private CodeTree createReturnSpecializeAndExecute(CodeTreeBuilder parent, ExecutableTypeData executable, SpecializationData nextSpecialization, ActualParameter exceptionParam) {
            CodeTreeBuilder specializeCall = new CodeTreeBuilder(parent);
            specializeCall.startCall("specializeAndExecute");
            specializeCall.string(nodeSpecializationClassName(nextSpecialization) + ".class");
            addInternalValueParameterNames(specializeCall, executable, nextSpecialization.getNode().getGenericSpecialization(), exceptionParam != null ? exceptionParam.getLocalName() : null, true,
                            true);
            specializeCall.end().end();

            CodeTreeBuilder builder = new CodeTreeBuilder(parent);
            builder.startReturn();
            builder.tree(createExpectType(nextSpecialization.getNode(), executable, specializeCall.getRoot()));
            builder.end();

            return builder.getRoot();
        }

        private void buildSpecializeAndExecute(CodeTypeElement clazz, SpecializationData specialization) {
            NodeData node = specialization.getNode();
            TypeData returnType = node.getGenericSpecialization().getReturnType().getTypeSystemType();

            CodeExecutableElement method = new CodeExecutableElement(modifiers(PRIVATE), returnType.getPrimitiveType(), "specializeAndExecute");
            method.addParameter(new CodeVariableElement(getContext().getType(Class.class), "minimumState"));
            addInternalValueParameters(method, specialization.getNode().getGenericSpecialization(), true);
            clazz.add(method);

            CodeTreeBuilder builder = method.createBuilder();

            builder.tree(createDeoptimize(builder));
            emitSpecializationListeners(builder, specialization.getNode());

            builder.startStatement();
            builder.startCall("replace");
            builder.startCall(factoryClassName(specialization.getNode()), "specialize").string("this").string("minimumState");
            addInternalValueParameterNames(builder, specialization.getNode().getGenericSpecialization(), specialization.getNode().getGenericSpecialization(), null, true, true);
            builder.end();
            builder.end(); // call replace
            builder.end(); // statement

            String generatedMethodName;
            if (specialization.getNode().getGenericSpecialization().isUseSpecializationsForGeneric()) {
                generatedMethodName = generatedGenericMethodName(null);
            } else {
                generatedMethodName = generatedGenericMethodName(specialization.findNextSpecialization());
            }
            ExecutableElement generatedGeneric = clazz.getEnclosingClass().getMethod(generatedMethodName);

            CodeTreeBuilder genericExecute = CodeTreeBuilder.createBuilder();
            genericExecute.startCall(factoryClassName(specialization.getNode()), generatedMethodName);
            genericExecute.string("this");
            addInternalValueParameterNames(genericExecute, specialization.getNode().getGenericSpecialization(), specialization.getNode().getGenericSpecialization(), null, true, true);
            genericExecute.end(); // call generated generic

            CodeTree genericInvocation = genericExecute.getRoot();

            if (generatedGeneric != null && Utils.isVoid(generatedGeneric.getReturnType())) {
                builder.statement(genericInvocation);

                if (!Utils.isVoid(builder.findMethod().asType())) {
                    builder.startReturn().defaultValue(returnType.getPrimitiveType()).end();
                }
            } else {
                builder.startReturn().tree(genericInvocation).end();
            }
        }

    }

}
