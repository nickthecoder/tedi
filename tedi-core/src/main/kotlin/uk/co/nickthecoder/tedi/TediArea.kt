/*
 * Most of this code was copied (and convert from Java to Kotlin) from JavaFX's TextArea.
 * Therefore I have kept TextArea's copyright message. However much wasn't written by
 * Oracle, so don't blame them for my mistakes!!!
 *
 *
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package uk.co.nickthecoder.tedi

import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.css.*
import javafx.scene.AccessibleRole
import javafx.scene.Scene
import javafx.scene.control.Skin
import javafx.scene.control.TextInputControl
import uk.co.nickthecoder.tedi.javafx.ExpressionHelper
import uk.co.nickthecoder.tedi.javafx.ListListenerHelper
import uk.co.nickthecoder.tedi.javafx.NonIterableChange
import java.text.BreakIterator
import java.util.*

open class TediArea private constructor(protected val content: TediAreaContent)

    : TextInputControl(content) {

    constructor() : this(TediAreaContent())


    constructor(text: String) : this(TediAreaContent()) {
        this.text = text
    }

    constructor(sharedContent: TediArea) : this(sharedContent.content)


    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // Paragraphs
    private val paragraphsProperty = ReadOnlyListWrapper(content.paragraphList())

    private fun paragraphsProperty(): ReadOnlyListProperty<CharSequence> = paragraphsProperty

    val paragraphs: ObservableList<CharSequence>
        get() = content.paragraphList()


    // Line Count
    private val lineCountProperty = Bindings.size(paragraphsProperty)!!

    fun lineCountProperty() = lineCountProperty

    val lineCount
        get() = lineCountProperty().get()


    // Display Line Numbers
    private val displayLineNumbersProperty = object : StyleableBooleanProperty(false) {

        override fun getBean() = this@TediArea
        override fun getName() = "displayLineNumbers"
        override fun getCssMetaData() = StyleableProperties.DISPLAY_LINE_NUMBERS
    }

    fun displayLineNumbersProperty() = displayLineNumbersProperty

    /**
     * Determines if line numbers are displayed.
     * This can also be set using the css : -fx-display-line-numbers
     */
    var displayLinesNumbers: Boolean
        get() = displayLineNumbersProperty.get()
        set(v) {
            displayLineNumbersProperty.set(v)
        }


    // Pref Column Count
    private val prefColumnCountProperty = object : StyleableIntegerProperty(DEFAULT_PREF_COLUMN_COUNT) {

        private var oldValue = get()

        override fun invalidated() {
            val value = get()
            if (value < 0) {
                if (isBound) {
                    unbind()
                }
                set(oldValue)
                throw IllegalArgumentException("value cannot be negative.")
            }
            oldValue = value
        }

        override fun getCssMetaData() = StyleableProperties.PREF_COLUMN_COUNT
        override fun getBean() = this@TediArea
        override fun getName() = "prefColumnCount"
    }

    fun prefColumnCountProperty(): IntegerProperty = prefColumnCountProperty

    /**
     * The preferred number of columns of text (used to calculated TediArea's preferred width).
     */
    var prefColumnCount: Int
        get() = prefColumnCountProperty.get()
        set(v) {
            prefColumnCountProperty.set(v)
        }


    // Pref Row Count
    private val prefRowCountProperty = object : StyleableIntegerProperty(DEFAULT_PREF_ROW_COUNT) {

        private var oldValue = get()

        override fun invalidated() {
            val value = get()
            if (value < 0) {
                if (isBound) {
                    unbind()
                }
                set(oldValue)
                throw IllegalArgumentException("value cannot be negative.")
            }

            oldValue = value
        }

        override fun getCssMetaData(): CssMetaData<TediArea, Number> {
            return StyleableProperties.PREF_ROW_COUNT
        }

        override fun getBean(): Any {
            return this@TediArea
        }

        override fun getName(): String {
            return "prefRowCount"
        }
    }

    fun prefRowCountProperty(): IntegerProperty = prefRowCountProperty

    /**
     * The preferred number of row of text (used to calculated TediArea's preferred height).
     */
    var prefRowCount: Int
        get() = prefRowCountProperty.get()
        set(v) {
            prefRowCountProperty.set(v)
        }


    // Scroll Top
    private val scrollTopProperty = SimpleDoubleProperty(this, "scrollTop", 0.0)

    fun scrollTopProperty(): DoubleProperty = scrollTopProperty

    /**
     * The number of pixels by which the content is vertically scrolled.
     */
    var scrollTop: Double
        get() = scrollTopProperty.get()
        set(v) {
            scrollTopProperty.set(v)
        }

    // Scroll Left
    private val scrollLeftProperty = SimpleDoubleProperty(this, "scrollLeft", 0.0)

    fun scrollLeftProperty(): DoubleProperty = scrollLeftProperty

    /**
     * The number of pixels by which the content is horizontally scrolled.
     */
    var scrollLeft: Double
        get() = scrollLeftProperty.get()
        set(v) {
            scrollLeftProperty.set(v)
        }

    // Tab Inserts Spaces
    private val tabInsertsSpacesProperty = SimpleBooleanProperty(this, "tabInsertsSpaces", true)

    fun tabInsertsSpacesProperty(): BooleanProperty = tabInsertsSpacesProperty

    /**
     * If true, then the TAB key will insert spaces, rather than a tab-character.
     * The number of spaces is determined by the [indentSize] property.
     */
    var tabInsertsSpaces: Boolean
        get() = tabInsertsSpacesProperty.get()
        set(v) {
            tabInsertsSpacesProperty.set(v)
        }

    // Indent Size
    private val indentSizeProperty = SimpleIntegerProperty(this, "indentSize", 4)

    fun indentSizePropert(): IntegerProperty = indentSizeProperty

    var indentSize: Int
        get() = indentSizeProperty.get()
        set(v) {
            indentSizeProperty.set(v)
        }

    /**
     * The Break Iterator to use, when double clicking, and also when using Left/Right Arrow + Shift.
     * The default value is :
     *     BreakIterator.getWordInstance()
     * which means that TediArea will behave in the same manner as TextArea.
     * However, for coding, set it to a [CodeWordBreakIterator].
     */
    var wordIterator: BreakIterator = BreakIterator.getWordInstance()


    /***************************************************************************
     *                                                                         *
     * End of Properties                                                       *
     *                                                                         *
     **************************************************************************/

    init {
        // Note, base class TextInputControl also adds "text-input" style class.
        styleClass.addAll("text-area", "tedi-area")

        accessibleRole = AccessibleRole.TEXT_AREA
    }

    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    override fun createDefaultSkin(): Skin<*> = TediAreaSkin(this)

    override fun getControlCssMetaData(): List<CssMetaData<out Styleable, *>> {
        return getClassCssMetaData()
    }

    /**
     * Returns a tab character (when [tabInsertsSpaces] == true), otherwise
     * n space characters, where n is taken from [indentSize].
     */
    fun tabIndentation() = if (tabInsertsSpaces) " ".repeat(indentSize) else "\t"


    /***************************************************************************
     *                                                                         *
     * Word Selection. These are basically an EXACT copy of those from         *
     * TextInputControl, except that I need to access the private              *
     * wordIterator. And therefore, I've had to duplicate the lot. Grr.        *
     *                                                                         *
     **************************************************************************/

    protected fun previousWord(select: Boolean) {
        val textLength = length
        val text = text
        if (textLength <= 0) {
            return
        }

        wordIterator.setText(text)

        var pos = wordIterator.preceding(clamp(0, caretPosition, textLength))

        // Skip the non-word region, then move/select to the beginning of the word.
        while (pos != BreakIterator.DONE && !Character.isLetterOrDigit(text[clamp(0, pos, textLength - 1)])) {
            pos = wordIterator.preceding(clamp(0, pos, textLength))
        }

        // move/select
        selectRange(if (select) anchor else pos, pos)
    }

    protected fun nextWord(select: Boolean) {
        val textLength = length
        val text = text
        if (textLength <= 0) {
            return
        }

        wordIterator.setText(text)

        var last = wordIterator.following(clamp(0, caretPosition, textLength - 1))
        var current = wordIterator.next()

        // Skip whitespace characters to the beginning of next word, but
        // stop at newline. Then move the caret or select a range.
        while (current != BreakIterator.DONE) {
            for (p in last..current) {
                val ch = text[clamp(0, p, textLength - 1)]
                // Avoid using Character.isSpaceChar() and Character.isWhitespace(),
                // because they include LINE_SEPARATOR, PARAGRAPH_SEPARATOR, etc.
                if (ch != ' ' && ch != '\t') {
                    if (select) {
                        selectRange(anchor, p)
                    } else {
                        selectRange(p, p)
                    }
                    return
                }
            }
            last = current
            current = wordIterator.next()
        }

        // move/select to the end
        if (select) {
            selectRange(anchor, textLength)
        } else {
            end()
        }
    }

    protected fun endOfNextWord(select: Boolean) {
        val textLength = length
        val text = text
        if (textLength <= 0) {
            return
        }

        wordIterator.setText(text)

        var last = wordIterator.following(clamp(0, caretPosition, textLength))
        var current = wordIterator.next()

        // skip the non-word region, then move/select to the end of the word.
        while (current != BreakIterator.DONE) {
            for (p in last..current) {
                if (!Character.isLetterOrDigit(text[clamp(0, p, textLength - 1)])) {
                    if (select) {
                        selectRange(anchor, p)
                    } else {
                        selectRange(p, p)
                    }
                    return
                }
            }
            last = current
            current = wordIterator.next()
        }

        // move/select to the end
        if (select) {
            selectRange(anchor, textLength)
        } else {
            end()
        }
    }

    override fun previousWord() {
        previousWord(false)
    }

    override fun nextWord() {
        nextWord(false)
    }

    override fun endOfNextWord() {
        endOfNextWord(false)
    }

    override fun selectPreviousWord() {
        previousWord(true)
    }

    override fun selectNextWord() {
        nextWord(true)
    }

    override fun selectEndOfNextWord() {
        endOfNextWord(true)
    }


    /***************************************************************************
     *                                                                         *
     * ParagraphList class                                                     *
     *                                                                         *
     **************************************************************************/
    protected class ParagraphList(val content: TediAreaContent)

        : AbstractList<CharSequence>(), ObservableList<CharSequence> {

        override fun get(index: Int): CharSequence {
            return content.paragraphs.get(index)
        }

        override fun addAll(elements: Collection<CharSequence>): Boolean {
            throw UnsupportedOperationException()
        }

        override fun addAll(vararg paragraphs: CharSequence): Boolean {
            throw UnsupportedOperationException()
        }

        override fun setAll(paragraphs: Collection<CharSequence>): Boolean {
            throw UnsupportedOperationException()
        }

        override fun setAll(vararg paragraphs: CharSequence): Boolean {
            throw UnsupportedOperationException()
        }

        override val size: Int
            get() = content.paragraphs.size

        override fun addListener(listener: ListChangeListener<in CharSequence>) {
            content.listenerHelper = ListListenerHelper.addListener(content.listenerHelper, listener)
        }

        override fun removeListener(listener: ListChangeListener<in CharSequence>) {
            content.listenerHelper = ListListenerHelper.removeListener(content.listenerHelper, listener)
        }

        override fun removeAll(vararg elements: CharSequence): Boolean {
            throw UnsupportedOperationException()
        }

        override fun retainAll(vararg elements: CharSequence): Boolean {
            throw UnsupportedOperationException()
        }

        override fun remove(from: Int, to: Int) {
            throw UnsupportedOperationException()
        }

        override fun addListener(listener: InvalidationListener) {
            content.listenerHelper = ListListenerHelper.addListener<CharSequence>(content.listenerHelper, listener)
        }

        override fun removeListener(listener: InvalidationListener) {
            content.listenerHelper = ListListenerHelper.removeListener<CharSequence>(content.listenerHelper, listener)
        }
    }
    // End ParagraphList

    /***************************************************************************
     *                                                                         *
     * ParagraphListChange class                                               *
     *                                                                         *
     **************************************************************************/
    protected class ParagraphListChange(
            list: ObservableList<CharSequence>,
            from: Int,
            to: Int,
            private val removed: List<CharSequence>)

        : NonIterableChange<CharSequence>(from, to, list) {

        override fun getRemoved(): List<CharSequence> {
            return removed
        }

        override fun getPermutation(): IntArray {
            return IntArray(0)
        }
    }
    // End ParagraphListChange

    /***************************************************************************
     *                                                                         *
     * TediAreaContent class                                                   *
     *                                                                         *
     **************************************************************************/

    /**
     * You can think of this as a "Document".
     * It stores the text as a list of lines, where each line is stored as a
     * StringBuffer.
     *
     * This data structure allows largish text documents to be edited without
     * large String objects being created and then garbage collected.
     *
     * Note, whenever you use [Tedi.text], it builds a String object from this document.
     *
     * Alas, TextAreaSkin does NOT make best use of this structure, and instead uses
     * the string representation ([Tedi.text]) when making changes.
     * Therefore editing large documents is very inefficient.
     */
    protected class TediAreaContent : TextInputControl.Content {

        internal val paragraphs = mutableListOf<StringBuilder>(StringBuilder(DEFAULT_PARAGRAPH_CAPACITY))
        private var contentLength = 0
        private val paragraphList = ParagraphList(this)
        internal var listenerHelper: ListListenerHelper<CharSequence>? = null

        private var helper: ExpressionHelper<String>? = null

        fun paragraphList() = paragraphList

        override fun get(start: Int, end: Int): String {
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
                    textBuilder.append(paragraph[offset++])
                }

                i++
            }

            return textBuilder.toString()
        }

        override fun insert(index: Int, textIn: String?, notifyListeners: Boolean) {

            var text = textIn
            if (index < 0 || index > contentLength) {
                throw IndexOutOfBoundsException()
            }

            if (text == null) {
                throw IllegalArgumentException()
            }
            text = filterInput(text, false, false)
            val length = text.length
            if (length > 0) {
                // Split the text into lines
                val lines = ArrayList<StringBuilder>()

                var line = StringBuilder(DEFAULT_PARAGRAPH_CAPACITY)
                for (i in 0..length - 1) {
                    val c = text[i]

                    if (c == '\n') {
                        lines.add(line)
                        line = StringBuilder(DEFAULT_PARAGRAPH_CAPACITY)
                    } else {
                        line.append(c)
                    }
                }

                lines.add(line)

                // Merge the text into the existing content
                var paragraphIndex = paragraphs.size
                var offset = contentLength + 1

                var paragraph: StringBuilder

                do {
                    paragraph = paragraphs[--paragraphIndex]
                    offset -= paragraph.length + 1
                } while (index < offset)

                val start = index - offset

                val n = lines.size
                if (n == 1) {
                    // The text contains only a single line; insert it into the
                    // intersecting paragraph
                    paragraph.insert(start, line)
                    fireParagraphListChangeEvent(paragraphIndex, paragraphIndex + 1,
                            listOf<CharSequence>(paragraph))
                } else {
                    // The text contains multiple line; split the intersecting
                    // paragraph
                    val end = paragraph.length
                    val trailingText = paragraph.subSequence(start, end)
                    paragraph.delete(start, end)

                    // Append the first line to the intersecting paragraph and
                    // append the trailing text to the last line
                    val first = lines[0]
                    paragraph.insert(start, first)
                    line.append(trailingText)
                    fireParagraphListChangeEvent(paragraphIndex, paragraphIndex + 1,
                            listOf<CharSequence>(paragraph))

                    // Insert the remaining lines into the paragraph list
                    paragraphs.addAll(paragraphIndex + 1, lines.subList(1, n))
                    fireParagraphListChangeEvent(paragraphIndex + 1, paragraphIndex + n, emptyList())
                }

                // Update content length
                contentLength += length
                if (notifyListeners) {
                    ExpressionHelper.fireValueChangedEvent(helper)
                }
            }
        }

        override fun delete(start: Int, end: Int, notifyListeners: Boolean) {
            if (start > end) {
                throw IllegalArgumentException()
            }

            if (start < 0 || end > contentLength) {
                throw IndexOutOfBoundsException()
            }

            val length = end - start

            if (length > 0) {
                // Identify the trailing paragraph index
                var paragraphIndex = paragraphs.size
                var offset = contentLength + 1

                var paragraph: StringBuilder?

                do {
                    paragraph = paragraphs[--paragraphIndex]
                    offset -= paragraph.length + 1
                } while (end < offset)

                val trailingParagraphIndex = paragraphIndex
                val trailingOffset = offset
                val trailingParagraph = paragraph

                // Identify the leading paragraph index
                paragraphIndex++
                offset += paragraph!!.length + 1

                do {
                    paragraph = paragraphs[--paragraphIndex]
                    offset -= paragraph.length + 1
                } while (start < offset)

                val leadingParagraphIndex = paragraphIndex
                val leadingOffset = offset
                val leadingParagraph = paragraph

                // Remove the text
                if (leadingParagraphIndex == trailingParagraphIndex) {
                    // The removal affects only a single paragraph
                    leadingParagraph!!.delete(start - leadingOffset,
                            end - leadingOffset)

                    fireParagraphListChangeEvent(leadingParagraphIndex, leadingParagraphIndex + 1,
                            listOf<CharSequence>(leadingParagraph))
                } else {
                    // The removal spans paragraphs; remove any intervening paragraphs and
                    // merge the leading and trailing segments
                    val leadingSegment = leadingParagraph!!.subSequence(0,
                            start - leadingOffset)
                    val trailingSegmentLength = start + length - trailingOffset

                    trailingParagraph!!.delete(0, trailingSegmentLength)
                    fireParagraphListChangeEvent(trailingParagraphIndex, trailingParagraphIndex + 1,
                            listOf<CharSequence>(trailingParagraph))

                    if (trailingParagraphIndex - leadingParagraphIndex > 0) {
                        val removed = ArrayList<CharSequence>(paragraphs.subList(leadingParagraphIndex,
                                trailingParagraphIndex))
                        paragraphs.subList(leadingParagraphIndex,
                                trailingParagraphIndex).clear()
                        fireParagraphListChangeEvent(leadingParagraphIndex, leadingParagraphIndex,
                                removed)
                    }

                    // Trailing paragraph is now at the former leading paragraph's index
                    trailingParagraph.insert(0, leadingSegment)
                    fireParagraphListChangeEvent(leadingParagraphIndex, leadingParagraphIndex + 1,
                            listOf<CharSequence>(leadingParagraph))
                }

                // Update content length
                contentLength -= length
                if (notifyListeners) {
                    ExpressionHelper.fireValueChangedEvent(helper)
                }
            }

        }

        override fun length(): Int {
            return contentLength
        }

        override fun get(): String {
            return get(0, length())
        }

        override fun getValue(): String {
            return get()
        }

        override fun addListener(changeListener: ChangeListener<in String>) {
            helper = ExpressionHelper.addListener(helper, this, changeListener)
        }

        override fun removeListener(changeListener: ChangeListener<in String>) {
            helper = ExpressionHelper.removeListener(helper, changeListener)
        }

        override fun addListener(listener: InvalidationListener) {
            helper = ExpressionHelper.addListener(helper, this, listener)
        }

        override fun removeListener(listener: InvalidationListener) {
            helper = ExpressionHelper.removeListener(helper, listener)
        }

        private fun fireParagraphListChangeEvent(from: Int, to: Int, removed: List<CharSequence>) {
            val change = ParagraphListChange(paragraphList, from, to, removed)
            ListListenerHelper.fireValueChangedEvent(listenerHelper, change)
        }
    }
    // End of class TediAreaContent

    /***************************************************************************
     *                                                                         *
     * StyleableProperties object                                              *
     *                                                                         *
     **************************************************************************/
    private object StyleableProperties {

        val PREF_COLUMN_COUNT = object : CssMetaData<TediArea, Number>("-fx-pref-column-count",
                StyleConverter.getSizeConverter(), DEFAULT_PREF_COLUMN_COUNT) {

            override fun isSettable(n: TediArea): Boolean {
                return !n.prefColumnCountProperty().isBound()
            }

            override fun getStyleableProperty(n: TediArea): StyleableProperty<Number> {
                @Suppress("UNCHECKED_CAST")
                return n.prefColumnCountProperty() as StyleableProperty<Number>
            }
        }

        val PREF_ROW_COUNT = object : CssMetaData<TediArea, Number>("-fx-pref-row-count",
                StyleConverter.getSizeConverter(), DEFAULT_PREF_ROW_COUNT) {

            override fun isSettable(n: TediArea): Boolean {
                return !n.prefRowCountProperty().isBound()
            }

            override fun getStyleableProperty(n: TediArea): StyleableProperty<Number> {
                @Suppress("UNCHECKED_CAST")
                return n.prefRowCountProperty() as StyleableProperty<Number>
            }
        }

        val DISPLAY_LINE_NUMBERS = object : CssMetaData<TediArea, Boolean>("-fx-display-line-numbers",
                StyleConverter.getBooleanConverter(), false) {

            override fun isSettable(n: TediArea): Boolean {
                return !n.prefColumnCountProperty().isBound()
            }

            override fun getStyleableProperty(n: TediArea): StyleableProperty<Boolean> {
                @Suppress("UNCHECKED_CAST")
                return n.prefColumnCountProperty() as StyleableProperty<Boolean>
            }
        }

        val STYLEABLES: List<CssMetaData<out Styleable, *>>

        init {
            val styleables = ArrayList(TextInputControl.getClassCssMetaData())
            styleables.add(PREF_COLUMN_COUNT)
            styleables.add(PREF_ROW_COUNT)
            STYLEABLES = Collections.unmodifiableList(styleables)
        }
    }
    // End StyleableProperties

    /***************************************************************************
     *                                                                         *
     * Companion Object                                                        *
     *                                                                         *
     **************************************************************************/
    companion object {

        /**
         * The default value for [.prefColumnCount].
         */
        val DEFAULT_PREF_COLUMN_COUNT = 40

        /**
         * The default value for [.prefRowCount].
         */
        val DEFAULT_PREF_ROW_COUNT = 10

        val DEFAULT_PARAGRAPH_CAPACITY = 1000

        /**
         * A little utility method for stripping out unwanted characters.

         * @param txt
         * *
         * @param stripNewlines
         * *
         * @param stripTabs
         * *
         * @return The string after having the unwanted characters stripped out.
         */
        internal fun filterInput(text: String, stripNewlines: Boolean, stripTabs: Boolean): String {

            var result = text

            // Most of the time, when text is inserted, there are no illegal
            // characters. So we'll do a "cheap" check for illegal characters.
            // If we find one, we'll do a longer replace algorithm. In the
            // case of illegal characters, this may at worst be an O(2n) solution.
            // Strip out any characters that are outside the printed range
            if (containsInvalidCharacters(result, stripNewlines, stripTabs)) {
                val s = StringBuilder(result.length)
                for (i in 0..result.length - 1) {
                    val c = result[i]
                    if (!isInvalidCharacter(c, stripNewlines, stripTabs)) {
                        s.append(c)
                    }
                }
                result = s.toString()
            }
            return result
        }

        internal fun containsInvalidCharacters(text: String, newlineIllegal: Boolean, tabIllegal: Boolean): Boolean {
            for (i in 0..text.length - 1) {
                val c = text[i]
                if (isInvalidCharacter(c, newlineIllegal, tabIllegal)) return true
            }
            return false
        }

        private fun isInvalidCharacter(c: Char, newlineIllegal: Boolean, tabIllegal: Boolean): Boolean {
            if (c.toInt() == 0x7F) return true
            if (c.toInt() == 0xA) return newlineIllegal
            if (c.toInt() == 0x9) return tabIllegal
            if (c.toInt() < 0x20) return true
            return false
        }

        /**
         * @return The CssMetaData associated with this class, which may include the
         * CssMetaData of its super classes.
         */
        fun getClassCssMetaData(): List<CssMetaData<out Styleable, *>> {
            return StyleableProperties.STYLEABLES
        }

        fun style(scene: Scene) {
            val url = TediArea::class.java.getResource("tedi.css")
            scene.stylesheets.add(url.toExternalForm())
        }
    }
    // End Companion Object

}
