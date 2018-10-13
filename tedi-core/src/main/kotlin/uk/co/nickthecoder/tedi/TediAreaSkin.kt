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

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableIntegerValue
import javafx.css.CssMetaData
import javafx.css.Styleable
import javafx.css.StyleableBooleanProperty
import javafx.css.StyleableObjectProperty
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.IndexRange
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.util.Duration
import uk.co.nickthecoder.tedi.javafx.BehaviorSkinBase
import uk.co.nickthecoder.tedi.util.*

class TediAreaSkin(control: TediArea)

    : BehaviorSkinBase<TediArea, TediAreaBehavior>(control, TediAreaBehavior(control)) {


    internal val virtualView = VirtualView(control.paragraphs, ParagraphFactory(this))

    /**
     * A path, used to display the caret.
     */
    private val caretPath = Path()

    /**
     * Remembers horizontal position when traversing up / down.
     */
    private var targetCaretX = -1.0

    private val tmpText = Text()

    //---------------------------------------------------------------------------
    // Properties
    //---------------------------------------------------------------------------

    /**
     * The fill to use for the text under normal conditions
     */
    private val textFillProperty: StyleableObjectProperty<Paint> = createStyleable("textFill", Color.BLACK, TEXT_FILL)
    var textFill: Paint?
        get() = textFillProperty.get()
        set(v) = textFillProperty.set(v)

    /**
     * The background behind the selected text
     */
    private val highlightFillProperty: StyleableObjectProperty<Paint> = createStyleable("highlightFill", Color.DODGERBLUE, HIGHLIGHT_FILL)
    var highlightFill: Paint?
        get() = highlightFillProperty.get()
        set(v) = highlightFillProperty.set(v)

    /**
     * The selected text's color
     */
    private val highlightTextFillProperty: StyleableObjectProperty<Paint> = createStyleable("highlightTextFill", Color.WHITE, HIGHLIGHT_TEXT_FILL)
    var highlightTextFill: Paint?
        get() = highlightTextFillProperty.get()
        set(v) = highlightTextFillProperty.set(v)

    /**
     * The current line's background color
     */
    private val currentLineFillProperty: StyleableObjectProperty<Paint> = createStyleable("currentLineFill", Color.WHITE, CURRENT_LINE_FILL)
    var currentLineFill: Paint?
        get() = currentLineFillProperty.get()
        set(v) = currentLineFillProperty.set(v)

    private val displayCaretProperty: StyleableBooleanProperty = createStyleable("displayCaret", true, DISPLAY_CARET)
    var displayCaret: Boolean
        get() = displayCaretProperty.get()
        set(v) = displayCaretProperty.set(v)

    /**
     * The caret is visible when the text box is focused AND when the selection
     * is empty. If the selection is non empty or the text box is not focused
     * then we don't want to show the caret. Also, we show the caret while
     * performing some operations such as most key strokes. In that case we
     * simply toggle its opacity.
     *
     * Note, this value does NOT include the caret animation's on/off state.
     */
    private var caretVisible: ObservableBooleanValue = object : BooleanBinding() {
        init {
            bind(control.focusedProperty(), control.anchorProperty(), control.caretPositionProperty(),
                    control.disabledProperty(), control.editableProperty(), displayCaretProperty)
        }

        override fun computeValue(): Boolean {
            return displayCaretProperty.get() && control.isFocused &&
                    (control.caretPosition == control.anchor) &&
                    !control.isDisabled &&
                    control.isEditable
        }
    }

    private val caretAnimation = CaretAnimation(caretVisible, caretPath, control.caretPositionProperty())

    private var selectionHighlightRange: HighlightRange? = null

    //--------------------------------------------------------------------------
    // init
    //--------------------------------------------------------------------------

    init {

        // Initialize content
        children.addAll(virtualView, caretPath)

        // caretPath
        with(caretPath) {
            isManaged = false
            // TODO Should this be stroke?
            fillProperty().bind(textFillProperty)
        }

        // tmpText
        tmpText.fontProperty().bind(control.fontProperty())


        control.selectionProperty().addListener { _, oldValue, newValue ->
            onSelectionChanged(oldValue, newValue)
        }

        // Caret position
        control.caretPositionProperty().addListener { _, _, _ ->
            repositionCaret()
            scrollToCaret()
        }
        virtualView.hScroll.valueProperty().addListener { _, _, _ -> repositionCaret() }
        virtualView.vScroll.valueProperty().addListener { _, _, _ -> repositionCaret() }

        // Font
        control.fontProperty().addListener { _, _, new -> onFontChanged(new) }
        onFontChanged(control.font)

        // Gutter
        control.gutter().addListener { _, _, newValue ->
            if (control.displayLineNumbers) virtualView.gutter = newValue
        }
        control.displayLineNumbers().addListener { _, _, newValue ->
            virtualView.gutter = if (newValue) control.gutter else null
        }
        virtualView.gutter = if (control.displayLineNumbers) control.gutter else null
    }

    //---------------------------------------------------------------------------
    // Methods
    //---------------------------------------------------------------------------

    /**
     * Uses a [HighlightRange]
     */
    private fun onSelectionChanged(oldValue: IndexRange, newValue: IndexRange) {

        if (oldValue.length == 0 && newValue.length == 0) return

        selectionHighlightRange?.let { skinnable.highlightRanges().remove(it) }
        selectionHighlightRange = if (newValue.length == 0) null else HighlightRange(newValue.start, newValue.end, selectionHighlight)
        selectionHighlightRange?.let { skinnable.highlightRanges().add(it) }

    }

    private fun repositionCaret() {
        targetCaretX = -1.0

        val (line, column) = skinnable.lineColumnForPosition(skinnable.caretPosition)
        val paragraphNode = getParagraphNode(line)
        if (paragraphNode == null) {
            caretPath.layoutX = -100.0 // Off screen
        } else {
            caretPath.layoutY = paragraphNode.boundsInParent.maxY
            val (x, font) = paragraphNode.caretDetails(column)
            if (font != null && font.size != caretFontSize) {
                createCaretPath(font)
            }
            caretPath.layoutX = x
        }
    }

    private var caretFontSize = 0.0

    private fun createCaretPath(font: Font) {
        caretFontSize = font.size
        val height = caretFontSize * 1.3 // Font API is lacking. It doesn't give us font metrics, so lets guess a height.
        caretPath.elements.clear()
        caretPath.elements.add(MoveTo(0.0, 0.0))
        caretPath.elements.add(LineTo(0.0, -height))
        caretPath.fillProperty().bind(textFillProperty)
        caretPath.strokeWidth = Math.min(1.0, caretFontSize / 15.0)
    }

    private fun onFontChanged(font: Font) {
        createCaretPath(font)
        virtualView.reset()
        Platform.runLater { repositionCaret() }
    }

    override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
        virtualView.resizeRelocate(contentX, contentY, contentWidth, contentHeight)
        // Position the caret.
        repositionCaret()
    }

    /**
     * Given a point within the tedi area, work out the position within the document.
     */
    fun positionForPoint(x: Double, y: Double): Int {
        val line = virtualView.getListIndexAtY(y)
        if (line < 0) {
            return 0
        }
        if (line >= skinnable.lineCount) {
            return skinnable.length
        }
        val paragraphNode = getParagraphNode(line)
        paragraphNode ?: throw IllegalStateException("Couldn't find paragraph node")

        val column = paragraphNode.getColumn(x)
        return skinnable.positionOfLine(line, column)

    }

    fun positionCaret(pos: Int, select: Boolean, extendSelection: Boolean) {
        if (select) {
            if (extendSelection) {
                skinnable.extendSelection(pos)
            } else {
                skinnable.selectPositionCaret(pos)
            }
        } else {
            skinnable.positionCaret(pos)
        }
    }

    /**
     * Ensures that the caret is visible, by altering the horizontal and vertical scroll bars
     */
    private fun scrollToCaret() {
        val (line, column) = skinnable.lineColumnForPosition(skinnable.caretPosition)
        virtualView.ensureItemVisible(line)
        virtualView.ensureXVisible(caretPath.layoutX)
    }

    /**
     * Move the caret up (or down if n < 1) n lines, keeping the caret in roughly the same X coordinate.
     * The desired X coordinate is stored in [targetCaretX], which is reset whenever the selection changes.
     */
    private fun changeLine(n: Int, select: Boolean) {

        val line = skinnable.lineForPosition(skinnable.caretPosition)
        val requiredX = if (targetCaretX < 0) caretPath.layoutX else targetCaretX
        val requiredLine = clamp(0, line + n, skinnable.lineCount - 1)
        val paragraphNode = getParagraphNode(requiredLine)

        val column = if (paragraphNode == null) {
            // Use the same column
            skinnable.caretColumn
        } else {
            paragraphNode.getColumn(requiredX)
        }

        val newPosition = skinnable.positionOfLine(requiredLine, column)

        if (select) {
            skinnable.selectRange(skinnable.anchor, newPosition)
        } else {
            skinnable.selectRange(newPosition, newPosition)
        }

        // targetCaretX will have been reset when the selection changed, therefore we need to set it again.
        targetCaretX = requiredX
    }

    fun previousPage(select: Boolean) {
        // TODO
    }

    fun nextPage(select: Boolean) {
        // TODO
    }

    fun previousLine(select: Boolean) {
        changeLine(-1, select)
    }

    fun nextLine(select: Boolean) {
        changeLine(1, select)
    }

    fun lineStart(select: Boolean) {
        val lineColumn = skinnable.lineColumnForPosition(skinnable.caretPosition)
        val newPosition = skinnable.positionOfLine(lineColumn.first, 0)
        if (select) {
            skinnable.selectRange(skinnable.anchor, newPosition)
        } else {
            skinnable.selectRange(newPosition, newPosition)
        }
    }

    fun lineEnd(select: Boolean) {
        val lineColumn = skinnable.lineColumnForPosition(skinnable.caretPosition)
        val newPosition = skinnable.positionOfLine(lineColumn.first, Int.MAX_VALUE)
        if (select) {
            skinnable.selectRange(skinnable.anchor, newPosition)
        } else {
            skinnable.selectRange(newPosition, newPosition)
        }
    }


    fun paragraphStart(previousIfAtStart: Boolean, select: Boolean) {
        val textArea = skinnable
        val text = textArea.textProperty().valueSafe
        var pos = textArea.caretPosition

        if (pos > 0) {
            if (previousIfAtStart && text.codePointAt(pos - 1) == 0x0a) {
                // We are at the beginning of a paragraph.
                // Back up to the previous paragraph.
                pos--
            }
            // Back up to the beginning of this paragraph
            while (pos > 0 && text.codePointAt(pos - 1) != 0x0a) {
                pos--
            }
            if (select) {
                textArea.selectPositionCaret(pos)
            } else {
                textArea.positionCaret(pos)
            }
        }
    }

    fun paragraphEnd(goPastInitialNewline: Boolean, goPastTrailingNewline: Boolean, select: Boolean) {
        val textArea = skinnable
        val text = textArea.textProperty().valueSafe
        var pos = textArea.caretPosition
        val len = text.length
        var wentPastInitialNewline = false

        if (pos < len) {
            if (goPastInitialNewline && text.codePointAt(pos) == 0x0a) {
                // We are at the end of a paragraph, start by moving to the
                // next paragraph.
                pos++
                wentPastInitialNewline = true
            }
            if (!(goPastTrailingNewline && wentPastInitialNewline)) {
                // Go to the end of this paragraph
                while (pos < len && text.codePointAt(pos) != 0x0a) {
                    pos++
                }
                if (goPastTrailingNewline && pos < len) {
                    // We are at the end of a paragraph, finish by moving to
                    // the beginning of the next paragraph (Windows behavior).
                    pos++
                }
            }
            if (select) {
                textArea.selectPositionCaret(pos)
            } else {
                textArea.positionCaret(pos)
            }
        }
    }

    fun deleteChar(previous: Boolean) {
        if (previous) {
            skinnable.deletePreviousChar()
        } else {
            skinnable.deleteNextChar()
        }
    }

    //--------------------------------------------------------------------------
    // CaretAnimation
    //--------------------------------------------------------------------------

    /**
     * [caretVisible] determines if the caret should be seen at all
     * (irrespective of the animation's on/off state).
     * The animation is stopped and started based on [caretVisible].
     *
     * [blinkProperty] is true if [caretVisible] is true AND the animation is
     * in its "ON" phase. Therefore the caret Node's opacity property
     * is bound to [blinkProperty]. We use the opacity, rather than visible
     * for efficiency (no additional layout pass).
     */
    private class CaretAnimation(
            val caretVisible: ObservableBooleanValue,
            caretNode: Node,
            caretPosition: ObservableIntegerValue) {

        private val animation = Timeline()

        private val blinkProperty = SimpleBooleanProperty()

        private var blinkOn: Boolean = false
            set(v) {
                field = v
                blinkProperty.value = caretVisible.get() && v
            }

        init {
            animation.cycleCount = Timeline.INDEFINITE
            animation.keyFrames.addAll(
                    KeyFrame(Duration.ZERO, EventHandler<ActionEvent> { blinkOn = true }),
                    KeyFrame(Duration.seconds(0.5), EventHandler<ActionEvent> { blinkOn = false }),
                    KeyFrame(Duration.seconds(1.0)))
            caretVisible.addListener { _, _, newValue ->
                if (newValue == true) {
                    blinkOn = true
                    animation.play()
                } else {
                    animation.stop()
                    blinkOn = false
                }
            }
            // modifying visibility of the caret forces a layout-pass (RT-32373), so
            // instead we modify the opacity.
            caretNode.opacityProperty().bind(object : DoubleBinding() {
                init {
                    bind(blinkProperty)
                }

                override fun computeValue(): Double {
                    return if (blinkProperty.get()) 1.0 else 0.0
                }
            })
            caretPosition.addListener { _, _, _ ->
                blinkOn = true
            }
        }
    }

    override fun getCssMetaData(): List<CssMetaData<out Styleable, *>> {
        return getClassCssMetaData()
    }

    private fun getParagraphNode(line: Int) = virtualView.getContentNode(line) as ParagraphFactory.ParagraphNode?

    //--------------------------------------------------------------------------
    // SelectionHighlight
    //--------------------------------------------------------------------------

    /**
     * A Highlight used by [selectionHighlightRange] to highlight the selected text.
     */
    private val selectionHighlight = object : FillHighlight {
        override fun style(rect: Rectangle) {
            rect.style = null
            rect.styleClass.clear()
            rect.fill = highlightFill
        }

        override fun style(text: Text) {
            text.fill = highlightTextFill
        }
    }

    //--------------------------------------------------------------------------
    // Companion Object
    //--------------------------------------------------------------------------

    companion object {

        val TEXT_FILL = createPaintCssMetaData<TediArea>("-fx-text-fill") { it.skin().textFillProperty }
        val HIGHLIGHT_FILL = createPaintCssMetaData<TediArea>("-fx-highlight-fill") { it.skin().highlightFillProperty }
        val HIGHLIGHT_TEXT_FILL = createPaintCssMetaData<TediArea>("-fx-highlight-text-fill") { it.skin().highlightTextFillProperty }
        val CURRENT_LINE_FILL = createPaintCssMetaData<TediArea>("-fx-current-line-fill") { it.skin().currentLineFillProperty }
        val DISPLAY_CARET = createBooleanCssMetaData<TediArea>("-fx-display-caret") { it.skin().displayCaretProperty }

        val STYLEABLES = listOf(
                TEXT_FILL, HIGHLIGHT_FILL, HIGHLIGHT_TEXT_FILL, CURRENT_LINE_FILL, DISPLAY_CARET)

        fun getClassCssMetaData() = STYLEABLES

    }
}

private fun TediArea.skin() = skin as TediAreaSkin
