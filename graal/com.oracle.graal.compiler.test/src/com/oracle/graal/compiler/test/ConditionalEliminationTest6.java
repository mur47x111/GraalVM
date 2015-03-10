/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.compiler.test;

import org.junit.*;

/**
 * Collection of tests for
 * {@link com.oracle.graal.phases.common.DominatorConditionalEliminationPhase} including those that
 * triggered bugs in this phase.
 */
public class ConditionalEliminationTest6 extends ConditionalEliminationTestBase {

    public static final A constA = new A();
    public static final B constB = new B();

    static class A {
    }

    static class B {
    }

    @SuppressWarnings("all")
    public static B reference1Snippet(Object a, B b) {
        if (a == constA) {
            return b;
        }
        return null;
    }

    @SuppressWarnings("all")
    public static B test1Snippet(Object a, B b) {
        if (a == constA) {
            if (a == null) {
                return null;
            } else {
                return b;
            }
        }
        return null;
    }

    @Test
    public void test1() {
        test("test1Snippet", "reference1Snippet");
    }

    @SuppressWarnings("all")
    public static B test2Snippet(Object a, B b) {
        if (a == constA) {
            if (a == constB) {
                return null;
            } else {
                return b;
            }
        }
        return null;
    }

    @Test
    public void test2() {
        test("test2Snippet", "reference1Snippet");
    }

    @SuppressWarnings("all")
    public static B test3Snippet(Object a, B b) {
        if (a == constA) {
            if (a == b) {
                return null;
            } else {
                return b;
            }
        }
        return null;
    }

    @Test
    public void test3() {
        test("test3Snippet", "reference1Snippet");
    }
}
