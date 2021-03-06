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
package uk.co.nickthecoder.tedi

import javafx.beans.InvalidationListener
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import uk.co.nickthecoder.tedi.ParagraphList.Paragraph
import uk.co.nickthecoder.tedi.javafx.ListListenerHelper
import uk.co.nickthecoder.tedi.util.*
import java.util.*

/**
 * The document is stored as a list of [Paragraph]s, split by new-line characters.
 * The [Paragraph] does not store the new-line characters (they are implied).
 *
 * Note, [TediArea.text] and [TediArea.textProperty] are backed by ParagraphList.
 * At no point does TediArea store the document as a String (or even a set of strings).
 */
class ParagraphList

    : AbstractList<Paragraph>(), ObservableList<Paragraph> {

    internal var listenerHelper: ListListenerHelper<Paragraph>? = null

    internal val paragraphs = mutableListOf(Paragraph(""))

    private val highlightRanges = FXCollections.observableList(mutableListOf<HighlightRange>())

    internal var contentLength = 0

    /**
     * The highest index into the [paragraphs] list with a valid [Paragraph.cachedPosition].
     * If < 0, then none are valid.
     */
    internal var validCacheIndex = 0

    init {
        highlightRanges.addListener { change: ListChangeListener.Change<out HighlightRange> ->
            highlightsChanged(change)
        }
    }

    override fun get(index: Int): Paragraph {
        return paragraphs[index]
    }

    override val size: Int
        get() = paragraphs.size


    override fun addListener(listener: ListChangeListener<in Paragraph>) {
        listenerHelper = ListListenerHelper.addListener(listenerHelper, listener)
    }

    override fun removeListener(listener: ListChangeListener<in Paragraph>) {
        listenerHelper = ListListenerHelper.removeListener(listenerHelper, listener)
    }

    override fun addListener(listener: InvalidationListener) {
        listenerHelper = ListListenerHelper.addListener<Paragraph>(listenerHelper, listener)
    }

    override fun removeListener(listener: InvalidationListener) {
        listenerHelper = ListListenerHelper.removeListener<Paragraph>(listenerHelper, listener)
    }


    override fun addAll(elements: Collection<Paragraph>): Boolean {
        return paragraphs.addAll(elements)
    }

    override fun addAll(vararg elements: Paragraph): Boolean {
        return paragraphs.addAll(elements)
    }

    override fun removeAt(index: Int): Paragraph {
        val result = paragraphs.removeAt(index)
        invalidateLineStartPosition(index)
        return result
    }


    override fun setAll(paragraphs: Collection<Paragraph>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun setAll(vararg paragraphs: Paragraph): Boolean {
        throw UnsupportedOperationException()
    }

    override fun removeAll(vararg elements: Paragraph): Boolean {
        throw UnsupportedOperationException()
    }

    override fun retainAll(vararg elements: Paragraph): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(from: Int, to: Int) {
        throw UnsupportedOperationException()
    }


    fun get(start: Int, end: Int): String {

        //if (end - start > 10000) { // TODO Remove once I'm confident I've removed all unwanted uses of getText()
        //    println("QUESTION. Is there still code that needlessly uses getText() ?")
        //    Thread.dumpStack()
        //}

        val length = end - start
        val textBuilder = StringBuilder(length)

        val paragraphCount = paragraphs.size

        var paragraphIndex = 0
        var offset = start

        while (paragraphIndex < paragraphCount) {
            val paragraph = paragraphs[paragraphIndex]
            val count = paragraph.length + 1

            if (offset < count) {
                break
            }

            offset -= count
            paragraphIndex++
        }

        // Read characters until end is reached, appending to text builder
        // and moving to next paragraph as needed
        var paragraph = paragraphs[paragraphIndex]

        var i = 0
        while (i < length) {
            if (offset == paragraph.length && i < contentLength) {
                textBuilder.append('\n')
                paragraph = paragraphs[++paragraphIndex]
                offset = 0
            } else {
                textBuilder.append(paragraph.charSequence[offset++])
            }

            i++
        }

        return textBuilder.toString()
    }

    internal fun delete(start: Int, end: Int) {
        if (start > end) {
            throw IllegalArgumentException()
        }

        if (start < 0 || end > contentLength) {
            throw IndexOutOfBoundsException()
        }

        // Adjust highlights.
        if (highlightRanges.isNotEmpty()) {
            val difference = end - start
            val i = highlightRanges.iterator()
            while (i.hasNext()) {
                val hr = i.next()

                if (hr.start == start && hr.end == end) {
                    // non stretchy ranges are deleted if they become zero length.
                    if (hr.stretchy) {
                        hr.end = hr.start // Make zero size
                    } else {
                        i.remove()
                    }
                } else if (hr.end <= start) { // BEFORE the deleted segment.
                    // Do nothing
                } else if (hr.start >= end) { // AFTER the deleted segment. Simple adjustment
                    hr.start -= difference
                    hr.end -= difference
                } else if (hr.start <= start && hr.end >= end) { // A superset of (or exactly the same as) the deletion.
                    hr.end -= difference
                } else if (hr.start >= start && hr.end <= end) { // within the deletion.
                    i.remove()
                } else if (hr.start < end && hr.end > end) { // Straddling the end of the deletion
                    val before = hr.start - start
                    hr.end -= end - hr.start + before
                    hr.start -= before
                } else if (hr.start < start && hr.end > start) { // Straddling the start of the deletion
                    hr.end = start
                } else {
                    throw IllegalStateException("Something weird has happened")
                }
            }
        }

        val length = end - start

        if (length > 0) {
            // Identify the trailing paragraph index
            val (leadingLine, leadingColumn) = lineColumnForPosition(start)
            val (trailingLine, trailingColumn) = lineColumnForPosition(end)

            val leadingParagraph = paragraphs[leadingLine]
            val trailingParagraph = paragraphs[trailingLine]

            if (leadingLine == trailingLine) {
                // The removal affects only a single paragraph
                invalidateLineStartPosition(leadingLine + 1)
                leadingParagraph.adjustHighlights()
                leadingParagraph.delete(leadingColumn, trailingColumn)
                fireParagraphUpdate(leadingLine)

            } else {

                if (leadingColumn != paragraphs[leadingLine].length || trailingColumn != paragraphs[trailingLine].length) {
                    // Remove the end part of the leadingParagraph, and add the trailing segment
                    val trailingSegment = trailingParagraph.charSequence.subSequence(trailingColumn, trailingParagraph.length)
                    invalidateLineStartPosition(leadingLine + 1)
                    leadingParagraph.delete(leadingColumn, leadingParagraph.length)
                    leadingParagraph.insert(leadingColumn, trailingSegment)
                    leadingParagraph.adjustHighlights(trailingParagraph.highlights.map { it.cause })
                    fireParagraphUpdate(leadingLine)
                }

                // Remove paragraphs
                val toRemove = paragraphs.subList(leadingLine + 1, trailingLine + 1)
                val removed = toRemove.toList()
                invalidateLineStartPosition(leadingLine + 1)
                toRemove.clear()
                fireParagraphRemove(leadingLine + 1, removed)
            }

            // Update content length
            contentLength -= length
        }
        /*
        if (!check()) {
            println("delete($start, $end) failed")
        }
        */
    }

    internal fun insert(position: Int, insertText: String) {
        var text = insertText
        if (position < 0 || position > contentLength) {
            throw IndexOutOfBoundsException()
        }

        text = filterInput(text, false, false)

        val length = text.length
        if (length > 0) {
            for (hr in highlightRanges) {

                if (position < hr.start) {
                    hr.start += length
                    hr.end += length
                } else if (position > hr.end) {
                } else if (position > hr.start && position < hr.end) {
                    hr.end += length
                } else {
                    // That's the simple cases taken care of! position must be straddling the start and/or end
                    if (hr.stretchy) {
                        if (position == hr.start && position == hr.end) {
                            hr.end += length
                        } else if (position == hr.end) {
                            hr.end += length
                        } else {
                            hr.end += length
                        }
                    } else {
                        // Not stretchy
                        if (position == hr.start) {
                            hr.start += length
                            hr.end += length
                        } else if (position == hr.end) {
                            // Do nothing
                        } else {
                            hr.end += length
                        }
                    }
                }
            }

            val lines = text.split("\n")
            val n = lines.size

            val (startLine, startColumn) = lineColumnForPosition(position)
            val startParagraph = paragraphs[startLine]

            if (n == 1) {
                // The text contains only a single line; insert it into the intersecting paragraph
                startParagraph.insert(startColumn, text)
                startParagraph.adjustHighlights()
                invalidateLineStartPosition(startLine + 1)
                fireParagraphUpdate(startLine)

            } else {
                // The text contains multiple lines; split the intersecting paragraph
                val trailingText = startParagraph.charSequence.subSequence(startColumn, startParagraph.length)

                // Remove the trailing part
                invalidateLineStartPosition(startLine + 1)
                startParagraph.delete(startColumn, startParagraph.length)

                // Append the first line to the intersecting paragraph
                invalidateLineStartPosition(startLine + 1)
                val startParagraphsHighlightRanges = startParagraph.highlights.map { it.cause }
                startParagraph.adjustHighlights()
                startParagraph.insert(startColumn, lines[0])
                fireParagraphUpdate(startLine)

                // Insert the remaining lines into the paragraph list
                invalidateLineStartPosition(startLine + 1)
                paragraphs.addAll(startLine + 1, lines.subList(1, n).map { Paragraph(it) })
                fireParagraphAdd(startLine + 1, startLine + n)

                // Add the trailing part which used to be in startParagraph.
                if (trailingText.isNotEmpty()) {
                    val lastIndex = startLine + n - 1
                    val lastParagraph = paragraphs[lastIndex]
                    invalidateLineStartPosition(lastIndex + 1)
                    positionOfLine(lastIndex) // Force the cachedPosition to become valid.
                    lastParagraph.insert(lastParagraph.length, trailingText)
                    lastParagraph.adjustHighlights(startParagraphsHighlightRanges)
                    fireParagraphUpdate(lastIndex)
                }
            }

            // Update content length
            contentLength += length
        }

        /*
        if (!check()) {
            println("insert($position, '$insertText') failed")
        }
        */

    }

    internal fun fireParagraphUpdate(from: Int, to: Int = from + 1) {
        ListListenerHelper.fireValueChangedEvent(listenerHelper, SimpleUpdateChange(this, from, to))
    }

    private fun fireParagraphAdd(from: Int, to: Int = from + 1) {
        ListListenerHelper.fireValueChangedEvent(listenerHelper, SimpleAddChange(this, from, to))
    }

    private fun fireParagraphRemove(from: Int, removed: List<Paragraph>) {
        ListListenerHelper.fireValueChangedEvent(listenerHelper, SimpleRemoveChange(this, from, removed))
    }


    private fun invalidateLineStartPosition(line: Int) {
        //println("Invalidating line $line (was $validCacheIndex now ${Math.min(validCacheIndex, line - 1)})")
        validCacheIndex = Math.min(validCacheIndex, line - 1)
    }

    /**
     * See [TediArea.positionOfLine]
     *
     * This is optimised from O(n) to O(1) (where n line number requested).
     * However, if you edit near the start of the document, then the next call will be O(n).
     */
    fun positionOfLine(line: Int): Int {
        if (validCacheIndex < line) {

            if (validCacheIndex < 0) {
                validCacheIndex = 0
                paragraphs[0].cachedPosition = 0
            }

            var total = paragraphs[validCacheIndex].cachedPosition + paragraphs[validCacheIndex].length + 1
            val requiredValidIndex = Math.min(line, paragraphs.size - 1)

            for (i in validCacheIndex + 1..requiredValidIndex) {
                val p = paragraphs[i]
                p.cachedPosition = total
                total += p.length + 1
            }
            //println("Validated to ${validCacheIndex}")
            validCacheIndex = requiredValidIndex
        }

        return paragraphs[line].cachedPosition
    }

    fun positionOfLine(line: Int, column: Int): Int {
        val lineStart = positionOfLine(line)
        if (line >= 0 && line < paragraphs.size) {
            return lineStart + clamp(0, paragraphs[line].length, column)
        } else {
            return lineStart
        }
    }

    /**
     * Used with the [lineForPosition] heuristic to guess a line number for a given position, in order to minimise
     * the number of [Paragraph]s that need to be checked before homing in on the correct line number.
     *
     * The must never be <= 0
     */
    private var guessCharsPerLine = 40.0


    /**
     * See [TediArea.lineForPosition]
     *
     * This is optimised from O(n) to O(1) (where n line number returned).
     * However, if you edit near the start of the document, then the next call will be O(n).
     *
     */
    fun lineForPosition(position: Int): Int {

        val guessedLine = clamp(0, (position / guessCharsPerLine).toInt(), paragraphs.size - 1)
        var count = positionOfLine(guessedLine)
        if (count == position) return guessedLine

        fun updateGuess(pos: Int, line: Int) {
            if (line > 20) {
                guessCharsPerLine = pos.toDouble() / line
            }
        }

        if (count < position) {

            // Move forwards
            for (i in guessedLine until paragraphs.size) {
                val p = paragraphs[i]
                if (count + p.length >= position) {
                    updateGuess(position, i)
                    return i
                }
                count += p.length + 1 // 1 for the new line character
            }
            updateGuess(position, paragraphs.size - 1)
            return paragraphs.size - 1

        } else {

            // Move backwards
            for (i in guessedLine - 1 downTo 0) {
                val p = paragraphs[i]
                count -= p.length + 1
                if (position >= count) {
                    updateGuess(position, i)
                    return i
                }
            }
            return 0
        }
    }

    /**
     * See [TediArea.lineColumnForPosition]
     */
    fun lineColumnForPosition(position: Int): Pair<Int, Int> {
        val line = lineForPosition(position)
        val linePos = positionOfLine(line)

        return Pair(line, clamp(0, position - linePos, paragraphs[line].length))
    }

    /**
     * Used during debugging to check that the cached lineStartPositions are correct.
     */
    internal fun check(): Boolean {
        println("Checking cached data")
        var count = 0
        for (i in 0..validCacheIndex) {
            if (count != paragraphs[i].cachedPosition) {
                println("Line $i actual=$count cached=${paragraphs[i].cachedPosition}\n")

                for (p in 0..validCacheIndex) {
                    print(paragraphs[p])
                }
                return false
            }
            count += paragraphs[i].length + 1
        }
        return true
    }

    fun highlightRanges(): ObservableList<HighlightRange> = highlightRanges

    /**
     * Called whenever [highlightRanges] are added/removed.
     */
    internal fun highlightsChanged(change: ListChangeListener.Change<out HighlightRange>) {
        // validate all cachedPositions.
        lineForPosition(contentLength)

        // We do this in two passes, so that paragraph change events are only
        // fired ONCE per paragraph. This holds the list of paragraph indices affected.
        val affectedParagraphs = mutableSetOf<Int>()

        /**
         * For each added HighlightRange, find which paragraphs it intersects, and
         * add that HighlightRange to those paragraphs.
         */
        fun add(list: List<HighlightRange>, from: Int, to: Int) {
            for (hrIndex in from..to - 1) {
                val hr = list[hrIndex]
                val fromP = lineForPosition(hr.from)
                val toP = lineForPosition(hr.to)
                positionOfLine(toP) // Force cachedPosition to become valid.
                for (pIndex in fromP..toP) {
                    val paragraph = paragraphs[pIndex]
                    paragraph.addHighlight(hr)
                    affectedParagraphs.add(pIndex)
                    hr.affectedParagraphs.add(paragraph)
                }
            }
        }

        /**
         * For each removed HighlightRange, find which paragraphs it intersects, and
         * remove it the HighlightRange from those paragraphs.
         */
        fun remove(removed: List<HighlightRange>) {
            removed.forEach { hr ->
                hr.affectedParagraphs.forEach { paragraph ->
                    // A deleted paragraph will have a -ve cachedPosition
                    if (paragraph.cachedPosition >= 0) {
                        val line = lineForPosition(paragraph.cachedPosition)
                        affectedParagraphs.add(line)
                        paragraph.removeHighlight(hr)
                    }
                }
            }
        }

        while (change.next()) {
            if (change.wasAdded()) {
                add(change.list, change.from, change.to)
            }
            if (change.wasRemoved()) {
                change.removed?.let { remove(it) }
            }
            if (change.wasUpdated()) {
                throw UnsupportedOperationException("Updating highlights is not supported.")
            }
            // I don't know what this kind of change is !?!
            if (change.wasPermutated()) {
                throw UnsupportedOperationException("Permuting highlights is not supported.")
            }
        }

        // The affected Paragraphs have now all been updated, fire changes, so that the Skin
        // can rebuild the Text objects.
        affectedParagraphs.forEach { i ->
            fireParagraphUpdate(i, i + 1)
        }
    }

    /**
     * Internally a paragraph is stored as a StringBuffer, however, never cast [charSequence] to StringBuffer,
     * because if you make changes to [charSequence], then bad things will happen!
     *
     * In addition to the StringBuffer, a Paragraph stores [cachedPosition], its position within the document.
     * So for if we have two paragraphs "Hello" and "World", then Hello will store a 0, and World will
     * store a 6 (5 plus 1 for the new-line character).
     * These numbers are lazily evaluated after changes to the document. For example, if we create a new
     * paragraph 3, then it (and all later Paragraphs) will have invalid [cachedPosition]s.
     *
     * [ParagraphList] keeps track of which [cachedPosition]s can be trusted via [validCacheIndex].
     * [validCacheIndex] is used to optimise conversion between line/column numbers and positions.
     */
    inner class Paragraph(private val buffer: StringBuffer) {

        constructor(text: CharSequence) : this(StringBuffer(text))

        val length
            get() = buffer.length

        val charSequence: CharSequence = buffer

        val text
            get() = buffer.toString()

        /**
         * Highlights that intersected this paragraph when they were added.
         * When [HighlightRange]s were added to the document, they were concerted
         * into [ParagraphHighlightRange]s.
         */
        internal val highlights = mutableListOf<ParagraphHighlightRange>()

        /**
         * The position of the start of this paragraph.
         * NOTE. This data becomes invalid, and must only be read via [ParagraphList.positionOfLine]
         */
        internal var cachedPosition: Int = 0

        internal fun insert(start: Int, str: CharSequence) {
            buffer.insert(start, str)
        }

        internal fun delete(start: Int, end: Int) {
            buffer.delete(start, end)
        }

        internal fun addHighlight(hr: HighlightRange) {
            // Convert the hr into a ParagraphHighlightRange
            val fromColumn = clamp(0, hr.from - cachedPosition, buffer.length)
            val toColumn = clamp(0, hr.to - cachedPosition, buffer.length)
            highlights.add(ParagraphHighlightRange(fromColumn, toColumn, hr))
        }

        internal fun removeHighlight(hr: HighlightRange) {
            highlights.removeIf { it.cause === hr }
        }

        /**
         * Called when some text has been added/removed from a single paragraph.
         */
        internal fun adjustHighlights() {
            for (phr in highlights) {
                val newStartColumn = phr.cause.from - cachedPosition
                val newEndColumn = phr.cause.to - cachedPosition

                // Is this highlight no longer applicable to this Paragraph?
                if (newEndColumn < 0 || newStartColumn >= length) {
                    phr.startColumn = -1
                } else {
                    phr.startColumn = clamp(0, newStartColumn, buffer.length)
                    phr.endColumn = clamp(0, newEndColumn, buffer.length)
                }
            }
            highlights.removeIf { it.startColumn < 0 }
        }

        /**
         * Called when inserting a multi-line piece of text, and therefore
         */
        internal fun adjustHighlights(otherHighlightRanges: List<HighlightRange>) {
            // Add those ranges that we don't already use
            otherHighlightRanges.filter { !containsHighlightRange(it) }.forEach { addHighlight(it) }

            // No adjust as normal (which may remove them again!)
            adjustHighlights()
        }

        private fun containsHighlightRange(hr: HighlightRange): Boolean {
            for (mine in highlights) {
                if (mine.cause === hr) {
                    return true
                }
            }
            return false
        }

        override fun toString() = "($cachedPosition) : $buffer\n"

    }
}
