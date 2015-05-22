/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.dsl.processor;

import com.oracle.truffle.api.TruffleLanguage.Registration;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes("com.oracle.truffle.api.*")
public final class LanguageRegistrationProcessor extends AbstractProcessor {
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    private void createProviderFile(TypeElement language, Registration annotation) {
        String filename = "META-INF/truffle/language";
        try {
            FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", filename, language);
            Properties p = new Properties();
            String className = processingEnv.getElementUtils().getBinaryName(language).toString();
            p.setProperty("name", annotation.name());
            p.setProperty("className", className);
            String[] mimes = annotation.mimeType();
            for (int i = 0; i < mimes.length; i++) {
                p.setProperty("mimeType." + i, mimes[i]);
            }
            try (OutputStream os = file.openOutputStream()) {
                p.store(os, "Generated by " + LanguageRegistrationProcessor.class.getName());
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage(), language);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(Registration.class)) {
            Registration annotation = e.getAnnotation(Registration.class);
            if (annotation != null && e.getKind() == ElementKind.CLASS) {
                if (!e.getModifiers().contains(Modifier.PUBLIC)) {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Registered language class must be public", e);
                }
                createProviderFile((TypeElement) e, annotation);
            }
        }

        return true;
    }
}
