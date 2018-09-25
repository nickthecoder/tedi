package uk.co.nickthecoder.tedi

import javafx.beans.InvalidationListener
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import uk.co.nickthecoder.tedi.ParagraphList.Paragraph
import uk.co.nickthecoder.tedi.javafx.ListListenerHelper
import java.util.*

/**
 * The document is stored as a list of [Paragraph]s, split by new-line characters.
 * The [Paragraph] does not store the new-line characters (they are implied).
 *
 * Note, TediArea's [text] and [textProperty] are backed by this ParagraphList (via [TediAreaContent]).
 * At no point does TediArea store the document as a String (or even a set of strings).
 */
class ParagraphList

    : AbstractList<Paragraph>(), ObservableList<Paragraph> {

    internal var listenerHelper: ListListenerHelper<Paragraph>? = null

    internal val paragraphs = mutableListOf(Paragraph(""))

    internal var contentLength = 0

    /**
     * The highest index into the [paragraphs] list with a valid [Paragraph.cachedPosition].
     * If < 0, then none are valid.
     */
    internal var validCacheIndex = 0

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


    private fun invalidateLineStartPosition(line: Int) {
        //println("Invalidating line $line (was $validCacheIndex now ${Math.min(validCacheIndex, line - 1)})")
        validCacheIndex = Math.min(validCacheIndex, line - 1)
    }

    fun get(start: Int, end: Int): String {

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
                textBuilder.append(paragraph.text[offset++])
            }

            i++
        }

        return textBuilder.toString()
    }

    fun getSequence(start: Int, end: Int): CharSequence = ContentCharSequence(start, end)

    fun delete(start: Int, end: Int) {
        if (start > end) {
            throw IllegalArgumentException()
        }

        if (start < 0 || end > contentLength) {
            throw IndexOutOfBoundsException()
        }

        val length = end - start

        if (length > 0) {
            // Identify the trailing paragraph index
            val (leadingLine, leadingColumn) = lineColumnFor(start)
            val (trailingLine, trailingColumn) = lineColumnFor(end)

            val leadingParagraph = paragraphs[leadingLine]
            val trailingParagraph = paragraphs[trailingLine]

            // Remove the text
            if (leadingLine == trailingLine) {
                // The removal affects only a single paragraph
                invalidateLineStartPosition(leadingLine + 1)
                leadingParagraph.delete(leadingColumn, trailingColumn)
                fireParagraphUpdate(leadingLine)

            } else {

                if (leadingColumn != paragraphs[leadingLine].length || trailingColumn != paragraphs[trailingLine].length) {
                    // Remove the end part of the leadingParagraph, and add the trailing segment
                    val trailingSegment = trailingParagraph.text.subSequence(trailingColumn, trailingParagraph.length)
                    invalidateLineStartPosition(leadingLine + 1)
                    leadingParagraph.delete(leadingColumn, leadingParagraph.length)
                    leadingParagraph.insert(leadingColumn, trailingSegment)
                    fireParagraphUpdate(leadingLine)
                }

                // Remove paragraphs
                //if (leadingLine + 1 - trailingLine > 0) {
                val toRemove = paragraphs.subList(leadingLine + 1, trailingLine + 1)
                val removed = toRemove.toList()
                invalidateLineStartPosition(leadingLine + 1)
                toRemove.clear()
                fireParagraphRemove(leadingLine + 1, removed)
                //}
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

    fun insert(position: Int, insertText: String) {

        var text = insertText
        if (position < 0 || position > contentLength) {
            throw IndexOutOfBoundsException()
        }

        text = filterInput(text, false, false)
        val length = text.length
        if (length > 0) {
            val lines = text.split("\n")
            val n = lines.size

            val (startLine, startColumn) = lineColumnFor(position)
            val startParagraph = paragraphs[startLine]

            if (n == 1) {
                // The text contains only a single line; insert it into the intersecting paragraph
                startParagraph.insert(startColumn, text)
                invalidateLineStartPosition(startLine + 1)
                fireParagraphUpdate(startLine)

            } else {
                // The text contains multiple lines; split the intersecting paragraph
                val trailingText = startParagraph.text.subSequence(startColumn, startParagraph.length)

                // Remove the trailing part
                invalidateLineStartPosition(startLine + 1)
                startParagraph.delete(startColumn, startParagraph.length)

                // Append the first line to the intersecting paragraph
                invalidateLineStartPosition(startLine + 1)
                startParagraph.insert(startColumn, lines[0])
                fireParagraphUpdate(startLine)

                // Insert the remaining lines into the paragraph list
                invalidateLineStartPosition(startLine + 1)
                paragraphs.addAll(startLine + 1, lines.subList(1, n).map { Paragraph(it) })
                fireParagraphAdd(startLine + 1, startLine + n)

                // Add the trailing part which used to be in startParagraph.
                if (trailingText.isNotEmpty()) {
                    val lastIndex = startLine + n - 1
                    invalidateLineStartPosition(lastIndex + 1)
                    paragraphs[lastIndex].insert(paragraphs[lastIndex].length, trailingText)
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

    private fun fireParagraphUpdate(from: Int, to: Int = from + 1) {
        ListListenerHelper.fireValueChangedEvent(listenerHelper, SimpleUpdateChange(this, from, to))
    }

    private fun fireParagraphAdd(from: Int, to: Int = from + 1) {
        ListListenerHelper.fireValueChangedEvent(listenerHelper, SimpleAddChange(this, from, to))
    }

    private fun fireParagraphRemove(from: Int, removed: List<Paragraph>) {
        ListListenerHelper.fireValueChangedEvent(listenerHelper, SimpleRemoveChange(this, from, removed))
    }

    /**
     * See [TediArea.lineStartPosition]
     *
     * This is optimised from O(n) to O(1) (where n line number requested).
     * However, if you edit near the start of the document, then the next call will be O(n).
     */
    fun lineStartPosition(line: Int): Int {
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
            //println("Valided to ${validCacheIndex}")
            validCacheIndex = requiredValidIndex
        }

        return paragraphs[line].cachedPosition
    }

    /**
     * See [TediArea.lineEndPosition]
     */
    fun lineEndPosition(line: Int) = lineStartPosition(line) + paragraphs[line].length

    fun positionFor(line: Int, column: Int): Int {
        val lineStart = lineStartPosition(line)
        if (line >= 0 && line < paragraphs.size) {
            return lineStart + clamp(0, paragraphs[line].length, column)
        } else {
            return lineStart
        }
    }

    /**
     * Used with the [lineFor] heuristic to guess a line number for a given position, in order to minimise
     * the number of [Paragraph]s that need to be checked before homing in on the correct line number.
     *
     * The must never be <= 0
     */
    private var guessCharsPerLine = 40

    /**
     * See [TediArea.lineFor]
     *
     * This is optimised from O(n) to O(1) (where n line number returned).
     * However, if you edit near the start of the document, then the next call will be O(n).
     *
     */
    fun lineFor(position: Int): Int {

        val guessedLine = clamp(0, position / guessCharsPerLine, paragraphs.size - 1)
        var count = lineStartPosition(guessedLine)
        if (count == position) return guessedLine

        if (count < position) {
            // Our guess was too low (and therefore guessCharsPerLine is too high)
            if (guessCharsPerLine > 1) {
                guessCharsPerLine--
            }
            // Move forwards
            for (i in guessedLine..paragraphs.size - 1) {
                val p = paragraphs[i]
                if (count + p.length >= position) {
                    return i
                }
                count += p.length + 1 // 1 for the new line character
            }
            return paragraphs.size - 1
        } else {

            // Our guess was too high (and therefore guessCharsPerLine is too low)
            if (position > 0) {
                guessCharsPerLine++
            }

            // Move backwards
            for (i in guessedLine - 1 downTo 0) {
                val p = paragraphs[i]
                count -= p.length + 1
                if (position >= count) {
                    return i
                }
            }
            return 0
        }
    }

    /**
     * See [TediArea.lineColumnFor]
     */
    fun lineColumnFor(position: Int): Pair<Int, Int> {
        val line = lineFor(position)
        val linePos = lineStartPosition(line)

        return Pair(line, clamp(0, position - linePos, paragraphs[line].length))
    }

    /**
     * Used during debugging to check that the cached lineStartPositions are correct.
     */
    fun check(): Boolean {
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

    /**
     * Internally a paragraph is stored as a StringBuffer, however, never cast [text] to StringBuffer,
     * because if you make changes to [text], then bad things will happen!
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
    inner class Paragraph(private val line: StringBuffer) {

        constructor(text: CharSequence) : this(StringBuffer(text))

        val length
            get() = line.length

        val text: CharSequence = line

        /**
         * Highlights that intersected this paragraph when they were added.
         * When [HighlightRange]s were added to the document, they were concerted
         * into [ParagraphHighlightRange]s.
         */
        internal val highlights = mutableListOf<ParagraphHighlightRange>()

        /**
         * The position of the start of this paragraph.
         * NOTE. This data becomes invalid, and must only be read via [ParagraphList.lineStartPosition]
         */
        internal var cachedPosition: Int = 0

        internal fun insert(start: Int, str: CharSequence) {
            line.insert(start, str)
        }

        internal fun delete(start: Int, end: Int) {
            line.delete(start, end)
        }

        override fun toString() = "($cachedPosition) : $text\n"

    }


    /**
     * A [CharSequence] for part of the document.
     *
     * NOTE. This is backed by the [ParagraphList], and therefore, if you
     * change the document, this CharSequence will reflect those changes.
     *
     * If you delete content, then [get] can return character 0x00 for
     * characters beyond the end of the document.
     */
    inner class ContentCharSequence(val startPosition: Int, endPosition: Int)
        : CharSequence {

        override val length = endPosition - startPosition

        override fun get(index: Int): Char {
            val (line, column) = lineColumnFor(startPosition + index)
            val paragraph = paragraphs[line]
            if (column < paragraph.length - 1) {
                return paragraph.text.get(column)
            } else {
                if (line < paragraphs.size - 1) {
                    return '\n'
                } else {
                    return '\u0000'
                }
            }
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return ContentCharSequence(startPosition + startIndex, startPosition + endIndex)
        }

        override fun toString(): String {
            val buffer = StringBuffer()
            for (i in 0..length - 1) {
                buffer.append(get(i))
            }
            return buffer.toString()
        }
    }

}
