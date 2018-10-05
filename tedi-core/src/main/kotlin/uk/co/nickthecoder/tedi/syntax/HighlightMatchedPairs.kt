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
