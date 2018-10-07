/*
 * Most of this code was copied (and convert to Kotlin) from JavaFX's VirtualFlow.
 * Therefore I have kept TextArea's copyright message. However much wasn't written by
 * Oracle, so don't blame them for my mistakes!!!
 *
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package uk.co.nickthecoder.tedi.util

import javafx.scene.Node
import javafx.scene.layout.Region
import javafx.scene.shape.Rectangle

/**
 * A simple extension to Region that ensures that anything wanting to render
 * outside of the bounds of the Region is clipped.
 */
class ClippedView(val node: Node) : Region() {

    init {
        children.add(node)
    }

    var clipX: Double = 0.0
        set(v) {
            field = v
            layoutX = -v
            clipRect.layoutX = v
        }

    var clipY: Double = 0.0
        set(v) {
            field = v
            layoutY = -v
            clipRect.layoutY = v
        }

    private val clipRect = Rectangle()

    init {
        styleClass.add("clipped-view")

        // clipping
        clipRect.isSmooth = false
        clip = clipRect
        // --- clipping

        super.widthProperty().addListener { _ -> clipRect.width = width }
        super.heightProperty().addListener { _ -> clipRect.height = height }

    }
}