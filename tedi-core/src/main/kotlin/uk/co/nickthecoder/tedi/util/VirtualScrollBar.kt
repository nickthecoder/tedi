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

    fun setSaveValue(v: Double) {
        value = clamp(0.0, v, max)
    }

    override fun decrement() {
        setSaveValue(value - 1)
    }

    override fun increment() {
        setSaveValue(value + 1)
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
