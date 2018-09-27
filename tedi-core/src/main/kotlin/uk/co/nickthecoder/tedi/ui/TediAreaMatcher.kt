package uk.co.nickthecoder.tedi.ui

import uk.co.nickthecoder.tedi.HighlightRange
import uk.co.nickthecoder.tedi.TediArea

/**
 *
 */
open class TediAreaMatcher(tediArea: TediArea)
    : AbstractMatcher<TediArea>(tediArea) {

    override fun clearMatches() {
        control.highlightRanges().removeAll(matches)
        control.highlightRanges().removeAll(replacements)
        super.clearMatches()
    }

    override fun addMatch(start: Int, end: Int): HighlightRange {
        val match = super.addMatch(start, end)
        control.highlightRanges().add(match)
        return match
    }

    override fun removeMatch(index: Int): HighlightRange {
        val hr = super.removeMatch(index)
        control.highlightRanges().remove(hr)
        return hr
    }

    override fun addReplacement(start: Int, end: Int): HighlightRange {
        val hr = super.addReplacement(start, end)
        control.highlightRanges().add(hr)
        return hr
    }

    override fun controlChanged(oldValue: TediArea?, newValue: TediArea?) {
        oldValue?.highlightRanges()?.removeAll(matches)
        oldValue?.highlightRanges()?.removeAll(replacements)
        super.controlChanged(oldValue, newValue)
    }

    override fun replaceAll(replacement: String) {
        control.highlightRanges().removeAll(matches)
        val newReplacements = matches.map { HighlightRange(it.start, it.end, replacementHighlight) }
        control.undoRedo.beginCompound()
        super.replaceAll(replacement)
        control.undoRedo.endCompound()
        control.highlightRanges().addAll(newReplacements)
    }
}

