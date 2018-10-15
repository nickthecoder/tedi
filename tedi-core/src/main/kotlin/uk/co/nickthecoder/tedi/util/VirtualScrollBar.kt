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
package uk.co.nickthecoder.tedi.util

import javafx.geometry.Orientation
import javafx.scene.control.ScrollBar

/**
 * The vertical scroll bar of a [VirtualView].
 * (The VirtualScroll's horizontal scroll bar is a regular [ScrollBar]).
 *
 * The [value] ranges from 0 to cellCount - visibleCells -1
 * i.e. [value] is the cell index at the top of the viewport
 *
 * The [increment] is 1 (scrolling up by one item in the virtual list)
 */
class VirtualScrollBar(val virtualView: VirtualView<*>)
    : ScrollBar() {

    init {
        orientation = Orientation.VERTICAL
    }

    var standardScrolling: Boolean = true

    fun setSafeValue(v: Double) {
        value = clamp(0.0, v, max)
    }

    override fun decrement() {
        setSafeValue(value - 1)
    }

    override fun increment() {
        setSafeValue(value + 1)
    }

    /**
     * Called when the mouse is clicked within the scroll bar.
     * Supports two mode :
     * - Standard : The scrolls up/down by a page
     * - Non-standard (and more sensible IMHO) : Jumps to where you clicked [blockIncrement] is ignored.
     */
    override fun adjustValue(position: Double) {

        if (standardScrolling) {
            if (position * max > value) {
                virtualView.pageDown()
            } else {
                virtualView.pageUp()
            }

        } else {
            // Make the scroll bar thumb's center become wherever was clicked.
            // i.e. Move to where I damn well clicked. The standard way is just stupid.
            value = clamp(0.0, (max + visibleAmount) * position - visibleAmount / 2, max)
        }
    }

}
