package uk.co.nickthecoder.tedi.syntax

import javafx.application.Platform
import uk.co.nickthecoder.tedi.FillStyleClassHighlight
import uk.co.nickthecoder.tedi.HighlightRange
import uk.co.nickthecoder.tedi.TediArea
import java.util.concurrent.CountDownLatch
import java.util.regex.Pattern

/**
 * Whenever the caret position changes, find the word at the caret, and highlight other instances of that word.
 * This is useful while programming, as you can see other uses of a variable/field/class.
 *
 * This is based on regular expressions, and therefore is unable to tell if a variable used in one part of the
 * code is actually the SAME variable used elsewhere.
 */
class HighlightIdenticalWords(val tediArea: TediArea, wait: Long = 100) {

    var myRanges = mutableListOf<HighlightRange>()

    private val listener = propertyChangeDelayedThread(tediArea.caretPositionProperty(), wait) {
        caretMoved()
    }

    /**
     * NOTE, this is NOT run on the JavaFX thread!
     */
    fun caretMoved() {
        val countdown = CountDownLatch(1)
        var text: String = ""
        var caretPosition: Int = 0

        Platform.runLater {
            text = tediArea.text
            caretPosition = tediArea.caretPosition
            countdown.countDown()
        }
        countdown.await()

        val newRanges = mutableListOf<HighlightRange>()

        val word = findWordAtPosition(text, caretPosition)
        if (word.length > 1) {
            val matcher = Pattern.compile("\\b${Pattern.quote(word)}\\b").matcher(text)

            while (matcher.find()) {
                if (matcher.group() == word) {
                    val range = if (caretPosition >= matcher.start() && caretPosition <= matcher.end()) {
                        createMatchingRange(matcher.start(), matcher.end())
                    } else {
                        createMatchedRange(matcher.start(), matcher.end())
                    }
                    newRanges.add(range)
                }
            }
        }

        Platform.runLater {
            clear()
            // Is there more than one instance of this word?
            if (newRanges.size > 1) {
                tediArea.highlightRanges().addAll(newRanges)
                myRanges = newRanges
            }
        }
    }

    fun createMatchedRange(start: Int, end: Int): HighlightRange {
        return HighlightRange(start, end, matchedHighlight)
    }

    fun createMatchingRange(start: Int, end: Int): HighlightRange {
        return HighlightRange(start, end, matchingHighlight)
    }

    fun findWordAtPosition(text: String, position: Int): String {
        var start = 0
        var end = text.length
        for (i in position - 1 downTo 0) {
            if (!isWordCharacter(text[i])) {
                start = i + 1
                break
            }
        }
        for (i in position..text.length - 1) {
            if (!isWordCharacter(text[i])) {
                end = i
                break
            }
        }
        return text.substring(start, end)
    }

    fun isWordCharacter(c: Char) = c.isJavaIdentifierPart()

    fun clear() {
        tediArea.highlightRanges().removeAll(myRanges)
        myRanges.clear()
    }

    fun detach() {
        clear()
        tediArea.caretPositionProperty().removeListener(listener)
    }

    companion object {
        val matchedHighlight = FillStyleClassHighlight("word-matched")
        val matchingHighlight = FillStyleClassHighlight("word-matching")
    }

}
