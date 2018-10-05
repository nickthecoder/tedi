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
import javafx.beans.binding.IntegerBinding
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableIntegerValue
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
import java.text.BreakIterator

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
 * - More methods to navigate around the document, such as [lineColumnForPosition], [positionForEvent] and [paragraphs].
 * - Locate the character position for a 2D point. See [positionForEvent].
 * - Highlight text (for syntax highlighting and while using find & replace).
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

    // *** Paragraphs ***
    fun paragraphsProperty(): ReadOnlyListProperty<Paragraph> = content.paragraphsProperty()

    val paragraphs: ObservableList<Paragraph> = content.paragraphsProperty().get()


    // *** Highlight Ranges ***
    /**
     * Note, highlightRanges are part of the content (AKA document), and therefore if you have two TediAreas
     * sharing the same content, they will also share the same highlightRanges.
     *
     * You CANNOT highlight only one TediArea when its content is shared with another TediArea.
     */
    fun highlightRanges() = content.highlightRanges()


    // *** caretLine ***
    private val caretLineProperty = SimpleIntegerProperty(this, "caretLine", 0)

    /**
     * Note, this is zero-based i.e. the first line is 0
     */
    fun caretLineProperty(): ReadOnlyIntegerProperty = caretLineProperty

    /**
     * Note, this is zero-based i.e. the first line is 0
     */
    val caretLine: Int
        get() = caretLineProperty.get()


    // *** caretColumn ***
    private val caretColumnProperty = SimpleIntegerProperty(this, "caretColumn", 0)

    /**
     * Note, this is zero-based i.e. the first column is 0
     */
    fun caretColumnProperty(): ReadOnlyIntegerProperty = caretColumnProperty

    /**
     * Note, this is zero-based i.e. the first column is 0
     */
    val caretColumn: Int
        get() = caretLineProperty.get()


    // *** Line Count ***
    private val lineCountProperty = Bindings.size(paragraphsProperty())!!

    fun lineCountProperty(): ObservableIntegerValue = lineCountProperty

    val lineCount
        get() = lineCountProperty.get()


    // *** Display Line Numbers ***
    private val displayLineNumbersProperty: StyleableBooleanProperty = object : StyleableBooleanProperty(false) {
        override fun getBean() = this@TediArea
        override fun getName() = "displayLineNumbers"
        override fun getCssMetaData() = DISPLAY_LINE_NUMBERS
    }

    fun displayLineNumbersProperty(): StyleableBooleanProperty = displayLineNumbersProperty

    /**
     * Determines if line numbers are displayed.
     * This can also be set using the css : -fx-display-line-numbers
     */
    var displayLineNumbers: Boolean
        get() = displayLineNumbersProperty.get()
        set(v) = displayLineNumbersProperty.set(v)


    // *** Scroll Top ***
    private val scrollTopProperty = SimpleDoubleProperty(this, "scrollTop", 0.0)

    fun scrollTopProperty(): DoubleProperty = scrollTopProperty

    /**
     * The number of pixels by which the content is vertically scrolled.
     */
    var scrollTop: Double
        get() = scrollTopProperty.get()
        set(v) = scrollTopProperty.set(v)


    // *** Scroll Left ***
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


    // *** Tab Inserts Spaces ***
    private val tabInsertsSpacesProperty = SimpleBooleanProperty(this, "tabInsertsSpaces", true)

    fun tabInsertsSpacesProperty(): BooleanProperty = tabInsertsSpacesProperty

    /**
     * If true, then the TAB key will insert spaces, rather than a tab-character.
     * The number of spaces is determined by the [indentSize] property.
     */
    var tabInsertsSpaces: Boolean
        get() = tabInsertsSpacesProperty.get()
        set(v) = tabInsertsSpacesProperty.set(v)


    // *** Indent Size ***
    private val indentSizeProperty = SimpleIntegerProperty(this, "indentSize", 4)

    fun indentSizeProperty(): IntegerProperty = indentSizeProperty

    var indentSize: Int
        get() = indentSizeProperty.get()
        set(v) = indentSizeProperty.set(v)


    // *** wordIterator ***
    private val wordIteratorProperty = SimpleObjectProperty(this, "wordIterator", BreakIterator.getWordInstance())

    fun wordIteratorProperty(): Property<BreakIterator> = wordIteratorProperty

    /**
     * The Break Iterator to use, when double clicking, and also when using Shift + Left/Right Arrow.
     * The default value is :
     *     BreakIterator.getWordInstance()
     * which means that TediArea will behave in the same manner as TextArea.
     *
     * However, for source code, set it to a [SourceCodeWordIterator].
     */
    var wordIterator: BreakIterator
        get() = wordIteratorProperty.get()
        set(v) = wordIteratorProperty.set(v)


    /***************************************************************************
     *                                                                         *
     * End of Properties                                                       *
     *                                                                         *
     **************************************************************************/

    init {
        // Note, base class TextInputControl also adds "text-input" style class.
        styleClass.addAll("text-area", "tedi-area")

        accessibleRole = AccessibleRole.TEXT_AREA

        caretLineProperty.bind(object : IntegerBinding() {
            init {
                bind(caretPositionProperty())
            }

            override fun computeValue() = lineForPosition(caretPosition)
        })

        caretColumnProperty.bind(object : IntegerBinding() {
            init {
                bind(caretPositionProperty())
            }

            override fun computeValue() = lineColumnForPosition(caretPosition).second
        })
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

    /**
     * Returns the position within [text] of the start of the nth line.
     * [line] and the returned result are zero based.
     */
    fun positionOfLine(line: Int) = content.positionOfLine(line)

    /**
     * Returns the position within [text] of the given line number and column.
     * Everything is zero based (so positionForLine(0,0) will return 0).
     */
    fun positionOfLine(line: Int, column: Int) = content.positionOfLine(line, column)

    /**
     * Returns the line number for the character position. This is "safe", i.e.
     *
     * If [position] < 0, then 0 is returned.
     *
     * if [position] >= [TediArea.text].length, then [TediArea.paragraphs].size -1 is returned.
     *
     * Therefore, the following is safe. 'line' is always a valid index into [TediArea.paragraphs] :
     *
     *     val line = tediArea.lineForPosition( position )
     *     val paragraph = tediArea.paragraphs[line] // Safe
     *
     * (Note that [TediArea.paragraphs] is never empty, it always has at least one [Paragraph]).
     */
    fun lineForPosition(position: Int) = content.lineForPosition(position)

    /**
     * Returns the line/column as a [Pair] for the given position within the [text].
     * Everything is zero-based. So Pair(0,0) relates to position 0 (the start of the text).
     *
     * If [position] < = 0, then (0,0) is returned.
     *
     * If [position] is beyond the end of the document, then
     * lineColumnForPosition( position ) == lineColumnForPosition( position -1 )
     *
     * Note that the column can range from 0 to the line's length, so for example :
     *
     *     val (line,column) = tediArea.lineColumnForPosition( position )
     *     val lineText = tediArea.getTextOfLine( line )
     *     val c1 = (lineText + "\n")[column] // Safe
     *     val c2 = lineText[column] // Dangerous! Can throw an exception
     *
     *  The last line can throw because 'getTextOfLine' does NOT include the new-line character, and
     *  'column' ranges from 0 to lineText.length INCLUSIVE.
     *  This is especially important for the last line of the document, which does NOT
     *  have an implied new line after it.
     */
    fun lineColumnForPosition(position: Int) = content.lineColumnForPosition(position)

    /**
     * Returns the character position for a give point, relative to TediArea's bounds.
     * You will usually get [x],[y] from a mouse event for a TediArea,
     * in which case [positionForEvent] may be easier to use!
     */
    fun positionForPoint(x: Double, y: Double): Int {
        return (skin as TediAreaSkin).positionForPoint(x, y)
    }

    fun positionForEvent(event: MouseEvent) = positionForPoint(event.x, event.y)


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

    /**
     * Avoid using Character.isSpaceChar() and Character.isWhitespace(),
     * because they include LINE_SEPARATOR, PARAGRAPH_SEPARATOR, etc.
     */
    private fun isWordSeparator(c: Char) = c == ' ' || c == '\t'

    protected fun previousWord(select: Boolean) {

        // TextInputControl's implementation used the whole text, which would be HORRIBLY inefficient for
        // large documents. Let's do better, by using a small string.
        // I'll assume a word isn't more than 100 characters, but I suppose a "better" solution could
        // look for the start of the previous non-blank line.
        val end = caretPosition
        val start = Math.max(end - 100, 0)
        if (end <= start) {
            return
        }
        val txt = getText(start, end)
        wordIterator.setText(txt)

        var pos = wordIterator.preceding(end - start)

        // Skip the non-word region, then move/select to the beginning of the word.
        while (pos != BreakIterator.DONE && isWordSeparator(txt[clamp(0, pos, txt.length - 1)])) {
            pos = wordIterator.preceding(clamp(0, pos, txt.length))
        }

        // move/select
        selectRange(if (select) anchor else start + pos, start + pos)
    }

    /**
     * This is used on Windows, where on Linux and MacOS, [endOfNextWord] is used.
     * I'm not sure what the difference is, so for now, I'll make use the linux/macOS version.
     */
    protected fun nextWord(select: Boolean) {
        endOfNextWord(select)
    }

    /**
     * FYI, I think this is badly named, as it moves to the end of the CURRENT word.
     * Alas, I cannot change it because its part of TextInputControl.
     *
     * Note, on Linux and MacOS, endOfNextWord is used where on Windows endOfWord is used.
     * I assume on windows, it also
     */
    protected fun endOfNextWord(select: Boolean) {
        // TextInputControl's implementation used the whole text, which would be HORRIBLY inefficient for
        // large documents. Let's do better, by using a string starting at the caret position.
        // I'll assume a word isn't more than 100 characters, but I suppose a "better" solution could
        // look for the end of the next non-blank line.
        val start = caretPosition
        val end = Math.min(start + 100, length)

        val txt = getText(start, end)
        wordIterator.setText(txt)

        var pos = wordIterator.following(0)
        while (pos != BreakIterator.DONE && isWordSeparator(txt[pos])) {
            pos = wordIterator.next()
        }

        if (pos == BreakIterator.DONE) {
            if (select) {
                selectRange(anchor, length - 1)
            } else {
                selectRange(length, length - 1)
            }
        } else {
            if (select) {
                selectRange(anchor, start + pos)
            } else {
                selectRange(start + pos, start + pos)
            }
        }
    }

    // NOTE. These are exact copies of the methods in TextInputControl,
    // but they MUST remain, because TextInputControl calls private methods.
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

        /**
         * As part of TextIntputControl's API, we need to fire events to listeners when the content changes.
         * This helps manage those listeners (add, remove, and fire).
         */
        private var helper: ExpressionHelper<String>? = null

        fun paragraphsProperty() = ReadOnlyListWrapper(paragraphList)

        fun highlightRanges() = paragraphList.highlightRanges()

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

        fun positionOfLine(line: Int) = paragraphList.positionOfLine(line)

        fun lineForPosition(position: Int) = paragraphList.lineForPosition(position)

        fun lineColumnForPosition(position: Int) = paragraphList.lineColumnForPosition(position)

        fun positionOfLine(line: Int, column: Int) = paragraphList.positionOfLine(line, column)


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
