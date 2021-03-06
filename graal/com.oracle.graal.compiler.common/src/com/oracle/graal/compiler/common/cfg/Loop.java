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

package com.oracle.graal.compiler.common.cfg;

import java.util.*;

public abstract class Loop<T extends AbstractBlockBase<T>> {

    private final Loop<T> parent;
    private final List<Loop<T>> children;

    private final int depth;
    private final int index;
    private final T header;
    private final List<T> blocks;
    private final List<T> exits;

    protected Loop(Loop<T> parent, int index, T header) {
        this.parent = parent;
        if (parent != null) {
            this.depth = parent.getDepth() + 1;
            parent.getChildren().add(this);
        } else {
            this.depth = 1;
        }
        this.index = index;
        this.header = header;
        this.blocks = new ArrayList<>();
        this.children = new ArrayList<>();
        this.exits = new ArrayList<>();
    }

    public abstract long numBackedges();

    @Override
    public String toString() {
        return "loop " + index + " depth " + getDepth() + (parent != null ? " outer " + parent.index : "");
    }

    public Loop<T> getParent() {
        return parent;
    }

    public List<Loop<T>> getChildren() {
        return children;
    }

    public int getDepth() {
        return depth;
    }

    public int getIndex() {
        return index;
    }

    public T getHeader() {
        return header;
    }

    public List<T> getBlocks() {
        return blocks;
    }

    public List<T> getExits() {
        return exits;
    }
}
