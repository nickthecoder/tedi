package uk.co.nickthecoder.tedi.syntax

import javafx.application.Platform
import javafx.beans.value.ChangeListener
import uk.co.nickthecoder.tedi.FillStyleHighlight
import uk.co.nickthecoder.tedi.HighlightRange
import uk.co.nickthecoder.tedi.TediArea
import uk.co.nickthecoder.tedi.util.propertyChangeDelayedThread

abstract class Syntax {

    abstract fun createRanges(text: String): List<HighlightRange>

    /**
     * Listen to changes to the tediArea's text, and recreates the HighlightRanges whenever the text changes.
     * The parsing is performed in a background thread after a short delay.
     *
     * @param wait Wait for an idle period before apply the highlighting. Default is 500 ms (half a second).
     *
     * @return A change listener, which can be passed to [detach], to stop syntax highlighting.
     */
    open fun attach(tediArea: TediArea, wait: Long = 500): ChangeListener<String> {
        val listener = propertyChangeDelayedThread(tediArea.textProperty(), wait) {

            val requiredRanges = createRanges(tediArea.text)
            val existingRanges = tediArea.highlightRanges().filter { it.owner === this }

            // This isn't exactly efficient (it's O(n*n)), but at least we are changing as few Paragraphs as possible.
            val newRanges = requiredRanges.filter { !existingRanges.contains(it) }
            val toRemove = existingRanges.filter { !requiredRanges.contains(it) }

            Platform.runLater {
                tediArea.highlightRanges().removeAll(toRemove)
                tediArea.highlightRanges().addAll(newRanges)
            }
        }
        return listener
    }

    /**
     * Stops syntax highlighting.
     *
     * @param listener The listener returned from attach.
     */
    open fun detach(tediArea: TediArea, listener: ChangeListener<String>) {
        tediArea.textProperty().removeListener(listener)
    }

    /**
     * Clears the highlighting caused by this highlighter.
     *
     * Note. this assumes that the [HighlightRange.owner] == this
     */
    open fun clear(tediArea: TediArea) {
        val toRemove = tediArea.highlightRanges().filter { it.owner === this }
        tediArea.highlightRanges().removeAll(toRemove)
    }

    companion object {
        val ERROR_HIGHLIGHT = FillStyleHighlight("-fx-fill: black;", "-fx-fill: #ffcccc;")
    }
}
