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
package com.oracle.graal.replacements.nodes;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;

@NodeInfo
public final class BlackholeNode extends FixedWithNextNode implements LIRLowerable {

    @Input ValueNode value;

    public BlackholeNode(ValueNode value) {
        super(StampFactory.forVoid());
        this.value = value;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        gen.getLIRGeneratorTool().emitBlackhole(gen.operand(value));
    }

    @NodeIntrinsic
    @SuppressWarnings("unused")
    public static void consume(boolean v) {
    }

    @NodeIntrinsic
    @SuppressWarnings("unused")
    public static void consume(byte v) {
    }

    @NodeIntrinsic
    @SuppressWarnings("unused")
    public static void consume(short v) {
    }

    @NodeIntrinsic
    @SuppressWarnings("unused")
    public static void consume(char v) {
    }

    @NodeIntrinsic
    @SuppressWarnings("unused")
    public static void consume(int v) {
    }

    @NodeIntrinsic
    @SuppressWarnings("unused")
    public static void consume(long v) {
    }

    @NodeIntrinsic
    @SuppressWarnings("unused")
    public static void consume(float v) {
    }

    @NodeIntrinsic
    @SuppressWarnings("unused")
    public static void consume(double v) {
    }

    @NodeIntrinsic
    @SuppressWarnings("unused")
    public static void consume(Object v) {
    }
}
