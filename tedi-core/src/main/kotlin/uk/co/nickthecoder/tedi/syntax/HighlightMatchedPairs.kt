package uk.co.nickthecoder.tedi.syntax

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import uk.co.nickthecoder.tedi.*

/**
 * When the caret position is within a [PairedHighlightRange], highlight the other
 * half of the pair.
 */
open class HighlightMatchedPairs(val tediArea: TediArea, val pairHighlight: Highlight, val errorHighlight: Highlight) : ChangeListener<Number> {

    constructor(tediArea: TediArea) : this(tediArea, FillStyleClassHighlight("syntax-pair"), FillStyleClassHighlight("syntax-error"))

    val myRanges = mutableListOf<HighlightRange>()

    init {
        tediArea.caretPositionProperty().addListener(this)
    }

    override fun changed(observable: ObservableValue<out Number>?, oldValue: Number?, newValue: Number?) {
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
        tediArea.caretPositionProperty().removeListener(this)
    }
}
