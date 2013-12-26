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
package com.oracle.truffle.sl;

import java.math.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.sl.runtime.*;

@TypeSystem({int.class, BigInteger.class, boolean.class, String.class, CallTarget.class, SLNull.class, Object[].class})
public class SLTypes {

    @TypeCheck
    public boolean isSLNull(Object value) {
        return SLNull.INSTANCE == value;
    }

    @TypeCast
    public SLNull asSLNull(Object value) {
        assert isSLNull(value);
        return SLNull.INSTANCE;
    }

    @ImplicitCast
    public BigInteger castBigInteger(int integer) {
        return BigInteger.valueOf(integer);
    }

}