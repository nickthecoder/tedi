package uk.co.nickthecoder.tedi.syntax

import javafx.application.Platform
import javafx.beans.value.ChangeListener
import uk.co.nickthecoder.tedi.HighlightRange
import uk.co.nickthecoder.tedi.StyleHighlight
import uk.co.nickthecoder.tedi.TediArea

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
            val ranges = createRanges(tediArea.text)
            Platform.runLater {
                clear(tediArea)
                tediArea.highlightRanges().addAll(ranges)
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
        tediArea.highlightRanges().removeAll(tediArea.highlightRanges().filter { it.owner === this })
    }


    companion object {
        val KEYWORD_STYLE = "KEYWORD" to StyleHighlight("-fx-fill: #000080;")
        val PAREN_STYLE = "PAREN" to StyleHighlight("-fx-fill: #cc33cc;")
        val BRACE_STYLE = "BRACE" to StyleHighlight("-fx-fill: #33cccc;")
        val BRACKET_STYLE = "BRACKET" to StyleHighlight("-fx-fill: #ddaa00;")
        val SEMICOLON_STYLE = "SEMICOLON" to StyleHighlight("-fx-fill: #ff6666;")
        val STRING_STYLE = "STRING" to StyleHighlight("-fx-fill: #008000;")
        val COMMENT_STYLE = "COMMENT" to StyleHighlight("-fx-fill: #808080;")
    }
}
