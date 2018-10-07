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
class VirtualScrollBar()
    : ScrollBar() {

    init {
        orientation = Orientation.VERTICAL
    }

    var standardScrolling: Boolean = true

    override fun decrement() {
        value -= 1
    }

    override fun increment() {
        value += 1
    }

    /**
     * Called when the mouse is clicked within the scroll bar.
     * Supports two mode :
     * - Standard : The scrolls up/down by a page
     * - Non-standard (and more sensible IMHO) : Jumps to where you clicked [blockIncrement] is ignored.
     */
    override fun adjustValue(position: Double) {
        if (standardScrolling) {
            // TODO This isn't quite right when scrolling UP, because the page above
            // won't always have the same number of nodes in it (when the node heights vary)
            // Instead, we should ask VirtualScroll to make the last visible cell the top-most,
            // or the first visible cell the bottom-most.
            super.adjustValue(position)
        } else {
            value = clamp(0.0, max * position, max)
        }
    }

}
