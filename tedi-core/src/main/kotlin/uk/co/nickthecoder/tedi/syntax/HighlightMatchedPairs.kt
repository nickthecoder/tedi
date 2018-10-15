/*
Tedi
Copyright (C) 2018 Nick Robinson

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2 only, as
published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
package uk.co.nickthecoder.tedi.syntax

import javafx.beans.value.ChangeListener
import uk.co.nickthecoder.tedi.*

/**
 * When the caret position is within a [PairedHighlightRange], highlight the other
 * half of the pair.
 */
open class HighlightMatchedPairs(val tediArea: TediArea, val pairHighlight: Highlight, val errorHighlight: Highlight) {

    constructor(tediArea: TediArea) : this(tediArea, FillStyleClassHighlight("syntax-pair"), FillStyleClassHighlight("syntax-error"))

    val myRanges = mutableListOf<HighlightRange>()

    private val caretPositionListener = ChangeListener<Number> { _, _, _ -> onCaretPositionChanged() }

    private val focusListener = ChangeListener<Boolean> { _, _, _ -> onFocusChanged() }

    init {
        tediArea.caretPositionProperty().addListener(caretPositionListener)
        tediArea.focusedProperty().addListener(focusListener)
    }

    open fun onCaretPositionChanged() {
        if (tediArea.isFocused) {
            checkPositionAndHighlight()
        }
    }

    open fun checkPositionAndHighlight() {
        val caret = tediArea.caretPosition

        clear()
        tediArea.highlightRanges().filterIsInstance(PairedHighlightRange::class.java).forEach { range ->
            if (caret >= range.start && caret <= range.end) {
                highlightOther(range)
            }
        }
        if (myRanges.isNotEmpty()) {
            tediArea.highlightRanges().addAll(myRanges)
        }

    }

    open fun onFocusChanged() {
        if (tediArea.isFocused) {
            checkPositionAndHighlight()
        } else {
            clear()
        }
    }

    open fun highlightOther(paired: PairedHighlightRange) {
        val newRangeA = HighlightRange(paired.start, paired.end, pairHighlight, this)
        val newRangeB = HighlightRange(paired.pairedWith.start, paired.pairedWith.end, pairHighlight, this)
        myRanges.add(newRangeA)
        myRanges.add(newRangeB)
    }

    open fun clear() {
        tediArea.highlightRanges().removeAll(myRanges)
        myRanges.clear()
    }

    fun detach() {
        clear()
        tediArea.caretPositionProperty().removeListener(caretPositionListener)
        tediArea.focusedProperty().removeListener(focusListener)
    }

}
