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
import javafx.css.CssMetaData
import javafx.css.StyleConverter
import javafx.css.Styleable
import javafx.css.StyleableBooleanProperty
import javafx.scene.AccessibleRole
import javafx.scene.Scene
import javafx.scene.control.TextArea
import javafx.scene.control.TextInputControl
import javafx.scene.input.MouseEvent
import uk.co.nickthecoder.tedi.ParagraphList.Paragraph
import uk.co.nickthecoder.tedi.javafx.ExpressionHelper
import uk.co.nickthecoder.tedi.javafx.ListListenerHelper
import java.text.BreakIterator
import java.util.*

/**
 * A control, similar to a [TextArea], which also extends [TextInputControl].
 *
 * TediArea is a simple text editor, which can be embedded in any JavaFX application,
 * and is particularly well suited as a source code editor.
 *
 * ## Improvements over TextArea :
 *
 * - Supports tabs and spaces as indentation.
 *   Blocks of code can be indented/un-indented using Tab and shift+Tab. See [tabInsertsSpaces].
 * - Better "word" selection for source code. See [wordIterator] and [SourceCodeWordIterator].
 * - Better undo/redo (see below).
 * - Can display line numbers. See [displayLineNumbers].
 * - More methods to navigate around the document, such as [lineColumnFor], [positionFor] and [paragraphs].
 * - Locate the character position for a 2D point. See [positionFor].
 * - Better word selection for source code, see [SourceCodeWordIterator].
 *
 * ## Limitations of TediArea
 *
 * - Accessibility features found in TextArea are missing from TediArea
 * - Right-to-left text may not work well, if at all! (I haven't tried it).
 * - Support for mobile devices, such as virtual keyboard are absent in TediArea (which are present in TextArea).
 * - Line wrapping is not currently supported
 * - Currently not optimised for large documents (neither is TextArea in JavaFX 8)
 *
 * ## Undo / Redo
 *
 * TediArea supports two undo/redo mechanisms, the default one uses [TextInputControl]
 * using methods [undo], [redo] etc.
 * IMHO, this is rather horrible to use, and is particularly bad for a combination of
 * edits, such as Find & Replace's "Replace All" (it creates many individual undos).
 *
 * For a better experience, use :
 *
 *     undoRedo = BetterUndoRedo()
 *
 * Alas, due to limitations imposed by [TextInputControl], TediArea cannot override the
 * usual [undo], [redo] methods. So instead, use :
 *
 *     myTediArea.undoRedo.undo()
 *
 * If you have set [undoRedo] to a [BetterUndoRedo], then the normal [undo], [redo]
 * methods will do nothing.
 * Because this can be confusing, the default behaviour uses [TextInputControl]'s
 * naff undo/redo implementation.
 *
 * ## Styling
 *
 * The control has style classes "text-input", "text-area" and "tedi-area".
 * "text-area" is added, even though it isn't a TextArea, so that a TediArea
 * will appear the same as a TextArea.
 *
 * There is a css file uk.co.nickthecoder.tedi/tedi.css, which can be added to
 * the scene using : TediArea.style(scene)
 *
 * tedi.css includes a style ".tedi-area.code", which uses a fixed width font, and
 * turns on line numbering.
 *
 * You can style the line number gutter using ".tedi-area .gutter".
 *
 * ## Notes
 *
 * - Avoid frequent use of [text], and instead use [getSequence] where possible,
 *   as [text] converts the whole document to a String, which is expensive for large documents.
 */
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
    private val paragraphsProperty = content.paragraphsProperty()

    fun paragraphsProperty(): ReadOnlyListProperty<Paragraph> = paragraphsProperty

    val paragraphs = paragraphsProperty.get()

    // Highlight Ranges
    private val highlightRangesProperty = SimpleListProperty<HighlightRange>()

    fun highlightRangesProperty() = highlightRangesProperty

    val highlightRanges = highlightRangesProperty.get()


    // Line Count
    private val lineCountProperty = Bindings.size(paragraphsProperty())!!

    fun lineCountProperty() = lineCountProperty

    val lineCount
        get() = lineCountProperty.get()


    // Display Line Numbers
    private val displayLineNumbersProperty: StyleableBooleanProperty = object : StyleableBooleanProperty(false) {
        override fun getBean() = this@TediArea
        override fun getName() = "displayLineNumbers"
        override fun getCssMetaData() = DISPLAY_LINE_NUMBERS
    }

    fun displayLineNumbersProperty() = displayLineNumbersProperty

    /**
     * Determines if line numbers are displayed.
     * This can also be set using the css : -fx-display-line-numbers
     */
    var displayLineNumbers: Boolean
        get() = displayLineNumbersProperty.get()
        set(v) {
            displayLineNumbersProperty.set(v)
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

    fun indentSizeProperty(): IntegerProperty = indentSizeProperty

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
     * However, for coding, set it to a [SourceCodeWordIterator].
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

    override fun createDefaultSkin() = TediAreaSkin(this)

    /**
     * Returns a tab character (when [tabInsertsSpaces] == true), otherwise
     * n space characters, where n is taken from [indentSize].
     */
    fun tabIndentation() = if (tabInsertsSpaces) " ".repeat(indentSize) else "\t"

    fun getSequence(start: Int, end: Int) = content.getSequence(start, end)

    fun getSequence() = content.getSequence(0, length)

    fun getLine(line: Int) = content.getLine(line)

    /**
     * Returns the position within [text] of the start of the nth line.
     * [line] and the returned result are zero based.
     */
    fun lineStartPosition(line: Int) = content.lineStartPosition(line)

    /**
     * Returns the position within [text] of the end of the nth line.
     * [line] and the returned result are zero based.
     */
    fun lineEndPosition(line: Int) = content.lineEndPosition(line)

    /**
     * Returns the position within [text] of the given line number and column.
     * Everything is zero based (so positionFor(0,0) will return 0).
     */
    fun positionFor(line: Int, column: Int) = content.positionFor(line, column)

    /**
     * Returns the line number for the character position. This is "safe", i.e.
     *
     * If [position] < 0, then 0 is returned.
     *
     * if [position] >= [TediArea.text].length, then [TediArea.paragraphs].size -1 is returned.
     *
     * Therefore, the following is safe. 'line' is always a valid index into [TediArea.paragraphs] :
     *
     *     val line = tediArea.lineFor( position )
     *     val paragraph = tediArea.paragraphs[line] // Safe
     *
     * (Note that [TediArea.paragraphs] is never empty, it always has at least one [Paragraph]).
     */
    fun lineFor(position: Int) = content.lineFor(position)

    /**
     * Returns the line/column as a [Pair] for the given position within the [text].
     * Everything is zero-based. So Pair(0,0) relates to position 0 (the start of the text).
     *
     * If [position] < = 0, then (0,0) is returned.
     *
     * If [position] is beyond the end of the document, then
     * lineColumnFor( position ) == lineColumnFor( position -1 )
     *
     * Note that the column can range from 0 to the line's length, so for example :
     *
     *     val (line,column) = tediArea.lineColumnFor( position )
     *     val lineText = tediArea.getLine( line )
     *     val c1 = (lineText + "\n")[column] // Safe
     *     val c2 = lineText[column] // Dangerous! Can throw an exception
     *
     *  The last line can throw because 'getLine' does NOT include the new-line character, and
     *  'column' ranges from 0 to lineText.length INCLUSIVE.
     *  This is especially important for the last line of the document, which does NOT
     *  have an implied new line after it.
     */
    fun lineColumnFor(position: Int) = content.lineColumnFor(position)

    /**
     * Returns the character position for a give point, relative to TediArea's bounds.
     * You will usually get [x],[y] from a mouse event for a TediArea,
     * in which case [positionFor] may be easier to use!
     */
    fun positionForPoint(x: Double, y: Double): Int {
        return (skin as TediAreaSkin).positionForPoint(x, y)
    }

    fun positionFor(event: MouseEvent) = positionForPoint(event.x, event.y)


    /***************************************************************************
     *                                                                         *
     * TextInputControl has a rather limited (and crap) undo/redo facility.    *
     * It also uses private and final, so extending the existing functionality *
     * is impossible, so instead, I've created my own undo/redo, and           *
     * disabled the existing undo/redo features.                               *
     *                                                                         *
     **************************************************************************/

    /**
     * By default, TediArea uses the standard undo/redo of its base class
     * TextInputControl, which isn't very good!
     *
     * So, for better undo/redo, set this to [BetterUndoRedo]!
     * If you do though, be sure not to use [undo], [redo],
     * [undoableProperty], [redoableProperty], [isUndoable] or [isRedoable].
     * Instead, use the equivalents within [BetterUndoRedo].
     *
     * Alas, because InputTextControl uses private fields and final methods,
     * TediArea isn't able to use improve Undo/Redo in a more seamless fashion.
     * Sorry.
     */
    var undoRedo: UndoRedo = StandardUndoRedo(this)

    override fun selectRange(anchor: Int, caretPosition: Int) {
        super.selectRange(anchor, caretPosition)
        undoRedo.postChange()
    }

    override fun replaceText(start: Int, end: Int, text: String) {
        undoRedo.replaceText(start, end, text)
        super.replaceText(start, end, text)
        undoRedo.postChange()
    }

    /***************************************************************************
     *                                                                         *
     * Word Selection.                                                         *
     *                                                                         *
     **************************************************************************/

    protected fun previousWord(select: Boolean) {

        // TextInputControl's implementation used the whole text, which would be HORRIBLY inefficient for
        // large documents. Let's do better, by using a CharSequence.
        // I'll assume a word isn't more than 100 characters, but I suppose a "better" solution could
        // look for the start of the previous non-blank line.
        val end = caretPosition
        val start = Math.max(end - 100, 0)
        if (end <= start) {
            return
        }
        val cs = getSequence(start, end)

        // Grr, BreakIterator needs a CharacterIterator, and AFAIK they haven't provided one which works with a
        // CharSequence. Grr. I'm feeling lazy, so I'll just make a String. Annoying!
        val txt = cs.toString()
        wordIterator.setText(txt)

        var pos = wordIterator.preceding(end - start)

        // Skip the non-word region, then move/select to the beginning of the word.
        while (pos != BreakIterator.DONE && !Character.isLetterOrDigit(txt[clamp(0, pos, txt.length - 1)])) {
            pos = wordIterator.preceding(clamp(0, pos, txt.length))
        }

        // move/select
        selectRange(if (select) anchor else start + pos, start + pos)
    }

    protected fun nextWord(select: Boolean) {

        // TextInputControl's implementation used the whole text, which would be HORRIBLY inefficient for
        // large documents. Let's do better, by using a CharSequence starting at the caret position.
        // I'll assume a word isn't more than 100 characters, but I suppose a "better" solution could
        // look for the end of the next non-blank line.
        val start = caretPosition
        val end = Math.max(start + 100, length)
        val cs = getSequence(start, end)

        // Grr, BreakIterator needs a CharacterIterator, and AFAIK they haven't provided one which works with a
        // CharSequence. Grr. I'm feeling lazy, so I'll just make a String. Annoying!
        val txt = cs.toString()
        wordIterator.setText(txt)

        var last = wordIterator.following(0)
        var current = wordIterator.next()

        // Skip whitespace characters to the beginning of next word, but
        // stop at newline. Then move the caret or select a range.
        while (current != BreakIterator.DONE) {
            for (p in last..current) {
                val ch = txt[clamp(0, p, txt.length - 1)]
                // Avoid using Character.isSpaceChar() and Character.isWhitespace(),
                // because they include LINE_SEPARATOR, PARAGRAPH_SEPARATOR, etc.
                if (ch != ' ' && ch != '\t') {
                    if (select) {
                        selectRange(anchor, p + start)
                    } else {
                        selectRange(p + start, p + start)
                    }
                    return
                }
            }
            last = current
            current = wordIterator.next()
        }

        // move/select to the end
        if (select) {
            selectRange(anchor, length)
        } else {
            end()
        }
    }

    protected fun endOfNextWord(select: Boolean) {

        // TextInputControl's implementation used the whole text, which would be HORRIBLY inefficient for
        // large documents. Let's do better, by using a CharSequence starting at the caret position.
        // I'll assume a word isn't more than 100 characters, but I suppose a "better" solution could
        // look for the end of the next non-blank line.
        val start = caretPosition
        val end = Math.max(start + 100, length)
        val cs = getSequence(start, end)

        // Grr, BreakIterator needs a CharacterIterator, and AFAIK they haven't provided one which works with a
        // CharSequence. Grr. I'm feeling lazy, so I'll just make a String. Annoying!
        val txt = cs.toString()
        wordIterator.setText(txt)

        var last = wordIterator.following(0)
        var current = wordIterator.next()

        // skip the non-word region, then move/select to the end of the word.
        while (current != BreakIterator.DONE) {
            for (p in last..current) {
                if (!Character.isLetterOrDigit(txt[clamp(0, p, txt.length - 1)])) {
                    if (select) {
                        selectRange(anchor, p + start)
                    } else {
                        selectRange(p + start, p + start)
                    }
                    return
                }
            }
            last = current
            current = wordIterator.next()
        }

        // move/select to the end
        if (select) {
            selectRange(anchor, length)
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


    /**
     * You can think of this as a "Document", or "Model" in an MVC pattern
     * with [TediAreaSkin] as the View, and [TediAreaBehaviour] as the Controller.
     *
     * All changes to the document are made through this class (never using [ParagraphList] directly).
     * It extends the Content defined in TextInputControl.
     *
     * This is a thin wrapper around [ParagraphList], which does most of the hard work.
     */
    protected class TediAreaContent : TextInputControl.Content {

        private val paragraphList = ParagraphList()

        private var helper: ExpressionHelper<String>? = null

        fun paragraphsProperty() = ReadOnlyListWrapper(paragraphList)

        override fun insert(index: Int, insertText: String?, notifyListeners: Boolean) {
            insertText ?: throw IllegalArgumentException("insertText cannot be null")

            paragraphList.insert(index, insertText)
            if (notifyListeners && insertText.isNotEmpty()) {
                ExpressionHelper.fireValueChangedEvent(helper)
            }
        }

        override fun delete(start: Int, end: Int, notifyListeners: Boolean) {
            if (start != end) {
                paragraphList.delete(start, end)
                if (notifyListeners) {
                    ExpressionHelper.fireValueChangedEvent(helper)
                }
            }
        }

        override fun length(): Int {
            return paragraphList.contentLength
        }

        override fun getValue(): String {
            return get()
        }

        override fun get(): String {
            return get(0, length())
        }

        override fun get(start: Int, end: Int): String {
            return paragraphList.get(start, end)
        }

        fun getSequence(start: Int, end: Int) = paragraphList.getSequence(start, end)

        fun getLine(line: Int) = paragraphList[line].text


        fun lineStartPosition(line: Int) = paragraphList.lineStartPosition(line)

        fun lineEndPosition(line: Int) = paragraphList.lineEndPosition(line)

        fun lineFor(position: Int) = paragraphList.lineFor(position)

        fun lineColumnFor(position: Int) = paragraphList.lineColumnFor(position)

        fun positionFor(line: Int, column: Int) = paragraphList.positionFor(line, column)


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

    }
    // End of class TediAreaContent

    override fun getControlCssMetaData(): List<CssMetaData<out Styleable, *>> = getClassCssMetaData()

    /***************************************************************************
     *                                                                         *
     * Companion Object                                                        *
     *                                                                         *
     **************************************************************************/
    companion object {

        private val DISPLAY_LINE_NUMBERS = object : CssMetaData<TediArea, Boolean>("-fx-display-line-numbers",
                StyleConverter.getBooleanConverter(), false) {
            override fun isSettable(n: TediArea) = !n.displayLineNumbersProperty().isBound()
            override fun getStyleableProperty(n: TediArea) = n.displayLineNumbersProperty()
        }

        private val STYLEABLES: List<CssMetaData<out Styleable, *>> = extendList(TextInputControl.getClassCssMetaData(),
                DISPLAY_LINE_NUMBERS)

        fun getClassCssMetaData(): List<CssMetaData<out Styleable, *>> = STYLEABLES


        @JvmStatic fun style(scene: Scene) {
            val url = TediArea::class.java.getResource("tedi.css")
            scene.stylesheets.add(url.toExternalForm())
        }
    }
    // End Companion Object

}
