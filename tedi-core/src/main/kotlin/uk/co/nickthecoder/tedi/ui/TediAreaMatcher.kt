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

