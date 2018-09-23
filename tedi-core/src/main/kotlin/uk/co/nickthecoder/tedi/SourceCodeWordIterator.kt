/*
 * Most of this code was copied (and convert from Java to Kotlin) from JavaFX.
 * Therefore I have kept the copyright message. However much wasn't written by
 * Oracle, so don't blame them for my mistakes!!!
 *
 *
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package uk.co.nickthecoder.tedi

import java.text.BreakIterator
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.*

/**
 * This is better than the default word break iterator when used with source code, because
 * the default considers text such as : "foo.bar.baz" ONE word!
 *
 * Splits the text into three types : whitespace, words and everything else.
 * A word is considered to be made up of letters, numbers and underscores.
 * However, you can change this definition by using the constructor which takes a lambda.
 *
 * For example uk.co.nickthecoder.tedi will be split before and after each "."
 * Also "a += 2", the breaks will be 1, 2, 4, 5, 6. (i.e. "+=" is considered a word).
 *
 * This class was loosely based off of WhiteSpaceBasedBreakIterator.
 * This is horribly inefficient for large text, therefore do NOT use it large text.
 *
 * Note, if you need to change the definition of whitespace, create a sub-class and override [isWhiteSpace].
 */
open class SourceCodeWordIterator(val wordPartTest: (Char) -> Boolean)

    : BreakIterator() {

    /**
     * Uses the "default" definition of a word, which is letters, digits and underscores.
     * See the other constructor to choose your own definition.
     */
    constructor() : this({
        c: Char ->
        c.isLetterOrDigit() || c == '_'
    })

    private var text = CharArray(0)
    private var breaks = intArrayOf(0)
    private var pos = 0

    /**
     * I have made this an open method, so that sub-classes can change the definition of whitespace if they need to.
     */
    open fun isWhiteSpace(c: Char): Boolean = c.isWhitespace()

    /**
     * Calculate break positions eagerly parallel to reading text.
     */
    override fun setText(ci: CharacterIterator) {
        val begin = ci.beginIndex
        text = CharArray(ci.endIndex - begin)
        val breaks0 = IntArray(text.size + 1)
        var breakIndex = 0
        breaks0[breakIndex++] = begin

        var charIndex = 0
        var previousType: Int? = null // Null at the start, then 0=whitespace 1=word 2=non-word

        var c = ci.first()
        while (c != CharacterIterator.DONE) {
            text[charIndex] = c
            val currentType = if (isWhiteSpace(c)) 0 else if (wordPartTest(c)) 1 else 2

            if (previousType != null && previousType != currentType) {
                breaks0[breakIndex++] = charIndex + begin
            }
            previousType = currentType
            charIndex++
            c = ci.next()
        }
        if (text.isNotEmpty()) {
            breaks0[breakIndex++] = text.size + begin
        }
        breaks = IntArray(breakIndex)
        System.arraycopy(breaks0, 0, breaks, 0, breakIndex)
    }

    override fun getText(): CharacterIterator {
        return StringCharacterIterator(String(text))
    }

    override fun first(): Int {
        pos = 0
        return breaks[pos]
    }

    override fun last(): Int {
        pos = breaks.size - 1
        return breaks[pos]
    }

    override fun current(): Int {
        return breaks[pos]
    }

    override fun next(): Int {
        return if (pos == breaks.size - 1) BreakIterator.DONE else breaks[++pos]
    }

    override fun previous(): Int {
        return if (pos == 0) BreakIterator.DONE else breaks[--pos]
    }

    override fun next(n: Int): Int {
        return checkhit(pos + n)
    }

    override fun following(n: Int): Int {
        return adjacent(n, 1)
    }

    override fun preceding(n: Int): Int {
        return adjacent(n, -1)
    }

    private fun checkhit(hit: Int): Int {
        if (hit < 0 || hit >= breaks.size) {
            return BreakIterator.DONE
        } else {
            pos = hit
            return breaks[pos]
        }
    }

    private fun adjacent(n: Int, bias: Int): Int {
        val hit = Arrays.binarySearch(breaks, n)
        val offset = if (hit < 0) if (bias < 0) -1 else -2 else 0
        return checkhit(Math.abs(hit) + bias + offset)
    }
}
