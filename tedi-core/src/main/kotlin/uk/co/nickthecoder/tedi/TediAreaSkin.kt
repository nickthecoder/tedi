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
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableIntegerValue
import javafx.css.*
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.geometry.Rectangle2D
import javafx.geometry.VPos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import javafx.util.Duration
import uk.co.nickthecoder.tedi.javafx.BehaviorSkinBase
import uk.co.nickthecoder.tedi.util.*

class TediAreaSkin(control: TediArea)

    : BehaviorSkinBase<TediArea, TediAreaBehavior>(control, TediAreaBehavior(control)) {


    private val virtualView = VirtualView(control.paragraphs, ParagraphFactory())

    /**
     * A path, used to display the caret.
     */
    private val caretPath = Path()

    private val selectionHighlightGroup = Group()

    private var oldViewportBounds: Bounds = BoundingBox(0.0, 0.0, 0.0, 0.0)

    /**
     * Remembers horizontal position when traversing up / down.
     */
    private var targetCaretX = -1.0

    private val tmpText = Text()

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The fill to use for the text under normal conditions
     */
    private val textFillProperty: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.BLACK) {
        override fun getBean() = this@TediAreaSkin
        override fun getName() = "textFill"
        override fun getCssMetaData() = TEXT_FILL
    }
    var textFill: Paint?
        get() = textFillProperty.get()
        set(v) = textFillProperty.set(v)

    /**
     * The background behind the selected text
     */
    private val highlightFillProperty: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.DODGERBLUE) {
        override fun getBean() = this@TediAreaSkin
        override fun getName() = "highlightFill"
        override fun getCssMetaData() = HIGHLIGHT_FILL
    }
    var highlightFill: Paint?
        get() = highlightFillProperty.get()
        set(v) = highlightFillProperty.set(v)

    /**
     * The selected text's color
     */
    private val highlightTextFillProperty: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.WHITE) {
        override fun getBean() = this@TediAreaSkin
        override fun getName() = "highlightTextFill"
        override fun getCssMetaData() = HIGHLIGHT_TEXT_FILL
    }
    var highlightTextFill: Paint?
        get() = highlightTextFillProperty.get()
        set(v) = highlightTextFillProperty.set(v)

    /**
     * The current line's background color
     */
    private val currentLineFillProperty: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.WHITE) {
        override fun getBean() = this@TediAreaSkin
        override fun getName() = "currentLineFill"
        override fun getCssMetaData() = CURRENT_LINE_FILL
    }
    var currentLineFill: Paint?
        get() = currentLineFillProperty.get()
        set(v) = currentLineFillProperty.set(v)

    private val displayCaretProperty: StyleableBooleanProperty = object : StyleableBooleanProperty(true) {
        override fun getBean() = this@TediAreaSkin
        override fun getName() = "displayCaret"
        override fun getCssMetaData() = DISPLAY_CARET
    }
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

    //--------------------------------------------------------------------------
    // init
    //--------------------------------------------------------------------------

    init {

        // Initialize content
        children.addAll(virtualView, caretPath)
        // TODO children.addAll(currentLineRect, paragraphsGroup, selectionHighlightGroup, caretPath)

        // selection
        with(selectionHighlightGroup) {
            isManaged = false
        }

        // caretPath
        with(caretPath) {
            isManaged = false
            fillProperty().bind(textFillProperty)
            //strokeProperty().bind(textFillProperty)
        }

        // tmpText
        tmpText.fontProperty().bind(control.fontProperty())


        control.selectionProperty().addListener { _, _, _ ->
            onSelectionChanged()
        }

        // Caret position
        control.caretPositionProperty().addListener { _, _, _ -> onCaretMoved() }

        // Font
        control.fontProperty().addListener { _, _, _ -> onFontChanged() }
        onFontChanged()

        // Gutter
        control.gutterProperty().addListener { _, _, newValue -> if (control.displayLineNumbers) virtualView.gutter = newValue }
        control.displayLineNumbersProperty().addListener { _, _, newValue -> if (newValue) virtualView.gutter = control.gutter }

    }


    //---------------------------------------------------------------------------
    // Methods
    //---------------------------------------------------------------------------

    /**
     * Builds a selection, by creating Text objects (one per line) of the selection,
     * as well as a Rectangle for each of them.
     * This isn't efficient, as it clears the selection and rebuilds it every time.
     * This is expensive, when the selection is large.
     * Such as Select All, then ctrl+shift+Right lots of times!
     */
    private fun onSelectionChanged() {

        fun createText(str: String, x: Double, y: Double): Pair<Text, Rectangle> {
            val text = Text(str).apply {
                textOrigin = VPos.TOP
                wrappingWidth = 0.0
                styleClass.add("text")
                isManaged = false
                layoutX = x
                layoutY = y
                fontProperty().bind(skinnable.fontProperty())
                fillProperty().bind(highlightTextFillProperty)
            }

            val bounds = text.boundsInLocal
            val rectangle = Rectangle(bounds.width, bounds.height).apply {
                layoutX = text.layoutX
                layoutY = text.layoutY
                fillProperty().bind(highlightFillProperty)
            }
            return Pair(text, rectangle)
        }

        selectionHighlightGroup.children.forEach { child ->
            if (child is Text) {
                child.fontProperty().unbind()
                child.fillProperty().unbind()
            }
            if (child is Rectangle) {
                child.fillProperty().unbind()
            }
        }
        selectionHighlightGroup.children.clear()

        if (skinnable.selection.length != 0) {
            val (fromLine, fromColumn) = skinnable.lineColumnForPosition(skinnable.selection.start)
            val (toLine, toColumn) = skinnable.lineColumnForPosition(skinnable.selection.end)

            val firstLineText = skinnable.paragraphs[fromLine].charSequence
            tmpText.text = firstLineText.substring(0, fromColumn)

            if (fromLine == toLine) {

                val (text, background) = createText(firstLineText.substring(fromColumn, toColumn), tmpText.boundsInLocal.width, fromLine * lineHeight())
                selectionHighlightGroup.children.addAll(background, text)

            } else {

                // First line
                val firstLineTrailingText = firstLineText.substring(fromColumn)
                if (firstLineTrailingText.isNotEmpty()) {
                    val (text, background) = createText(firstLineTrailingText, tmpText.boundsInLocal.width, fromLine * lineHeight())
                    selectionHighlightGroup.children.addAll(background, text)
                }

                // Whole lines
                for (i in fromLine + 1..toLine - 1) { // Don't include the last line
                    val (text, background) = createText(skinnable.paragraphs[i].text, 0.0, i * lineHeight())
                    selectionHighlightGroup.children.addAll(background, text)
                }

                // Last line
                val lastLineLeadingText = skinnable.paragraphs[toLine].charSequence.substring(0, toColumn)
                if (lastLineLeadingText.isNotEmpty()) {
                    val (text, background) = createText(lastLineLeadingText, 0.0, toLine * lineHeight())
                    selectionHighlightGroup.children.addAll(background, text)
                }
            }

        }
    }

    private fun onCaretMoved() {
        targetCaretX = -1.0

        val (line, column) = skinnable.lineColumnForPosition(skinnable.caretPosition)
        val paragraphNode = getParagraphNode(line)
        if (paragraphNode == null) {
            caretPath.layoutX = -100.0 // Off screen
        } else {
            caretPath.layoutY = paragraphNode.layoutY
            caretPath.layoutX = paragraphNode.xForColumn(column)

            scrollCaretToVisible()
        }
    }

    private fun onFontChanged() {
        cachedLineHeight = 0.0
        caretPath.elements.clear()
        caretPath.elements.add(MoveTo(0.0, 0.0))
        caretPath.elements.add(LineTo(0.0, lineHeight()))
        caretPath.fillProperty().bind(textFillProperty)
        caretPath.strokeWidth = Math.min(1.0, lineHeight() / 15.0)
        virtualView.reset()
    }

    override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
        virtualView.resizeRelocate(contentX, contentY, contentWidth, contentHeight)
        // Position the caret.
        onCaretMoved()
    }

    internal fun previousPage(select: Boolean) {
        // TODO
    }

    internal fun nextPage(select: Boolean) {
        // TODO
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

    private fun scrollCaretToVisible() {
        // TODO
        val textArea = skinnable
        val bounds = caretPath.layoutBounds
        //val x = bounds.minX - textArea.scrollLeft + caretPath.layoutX
        //val y = bounds.minY - textArea.scrollTop + caretPath.layoutY
        val w = bounds.width
        val h = bounds.height

        if (w > 0 && h > 0) {
            //    scrollBoundsToVisible(Rectangle2D(x, y, w, h))
        }
    }

    private fun scrollBoundsToVisible(bounds: Rectangle2D) {
        // TODO
        /*
        val textArea = skinnable
        val viewportBounds = scrollPane.viewportBounds

        val viewportWidth = viewportBounds.width
        val viewportHeight = viewportBounds.height
        val scrollTop = textArea.scrollTop
        val scrollLeft = textArea.scrollLeft
        val slop = 6.0

        if (bounds.minY < 0) {
            var y = scrollTop + bounds.minY
            if (y <= contentView.snappedTopInset()) {
                y = 0.0
            }
            textArea.scrollTop = y
        } else if (contentView.snappedTopInset() + bounds.maxY > viewportHeight) {
            var y = scrollTop + contentView.snappedTopInset() + bounds.maxY - viewportHeight
            if (y >= getScrollTopMax() - contentView.snappedBottomInset()) {
                y = getScrollTopMax()
            }
            textArea.scrollTop = y
        }


        if (bounds.minX < 0) {
            var x = scrollLeft + bounds.minX - slop
            if (x <= contentView.snappedLeftInset() + slop) {
                x = 0.0
            }
            textArea.scrollLeft = x
        } else if (contentView.snappedLeftInset() + bounds.maxX > viewportWidth) {
            var x = scrollLeft + contentView.snappedLeftInset() + bounds.maxX - viewportWidth + slop
            if (x >= getScrollLeftMax() - contentView.snappedRightInset() - slop) {
                x = getScrollLeftMax()
            }
            textArea.scrollLeft = x
        }
        */
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

    fun previousLine(select: Boolean) {
        changeLine(-1, select)
    }

    fun nextLine(select: Boolean) {
        changeLine(1, select)
    }

    private var cachedLineHeight = 0.0

    fun lineHeight(): Double {
        if (cachedLineHeight == 0.0) {
            cachedLineHeight = Math.ceil(tmpText.boundsInLocal.height)
        }
        return cachedLineHeight
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


    //--------------------------------------------------------------------------
    // ParagraphFactory
    //--------------------------------------------------------------------------

    // TODO Reuse nodes (I got scrambled text when reusing)
    inner class ParagraphFactory : VirtualFactory {
        override fun createNode(index: Int): ParagraphNode {
            return ParagraphNode(index)
        }

        override fun itemChanged(index: Int, node: Node) {
            if (node is ParagraphNode) {
                node.update(index)
            }
        }
    }

    //--------------------------------------------------------------------------
    // ParagraphNode
    //--------------------------------------------------------------------------

    private fun getParagraphNode(line: Int) = virtualView.getContentNode(line) as ParagraphNode?

    inner class ParagraphNode(index: Int) : Group(), UpdatableNode {

        /**
         * If true, then the group contains a single, un-highlighted Text object.
         */
        private var isSimple = false

        init {
            update(index)
        }

        private fun createText(str: String): Text {
            return Text(str).apply {
                styleClass.add("text")
                textOrigin = VPos.TOP
                wrappingWidth = 0.0
                isManaged = false
                font = skinnable.font
                fill = textFillProperty.get()
            }
        }

        override fun update(newIndex: Int) {
            val paragraph = skinnable.paragraphs[newIndex]

            if (isSimple && paragraph.highlights.isEmpty()) {
                (children[0] as Text).text = paragraph.text
                return
            }

            children.clear()

            if (paragraph.highlights.isEmpty()) {

                isSimple = true
                children.add(createText(paragraph.text))

            } else {

                // Find all the boundaries between highlights.
                // Using a set, because if two highlights start at the same column, we only want that column
                // in the set once.
                val splits = mutableSetOf<Int>(0, paragraph.length)
                for (highlight in paragraph.highlights) {
                    splits.add(highlight.startColumn)
                    splits.add(highlight.endColumn)
                }
                val splitsList = splits.sorted()
                // Now we have a sorted list of column indices where the highlights change.

                // Create a Text object between each consecutive column indices in the list.
                var x = 0.0
                for (i in 0..splitsList.size - 2) {
                    val from = clamp(0, splitsList[i], paragraph.charSequence.length)
                    val to = clamp(0, splitsList[i + 1], paragraph.charSequence.length)

                    val text = createText(paragraph.charSequence.substring(from, to))
                    text.layoutX = x
                    val textBounds = text.boundsInLocal

                    // We may not need a background color, so don't create a Rectangle yet.
                    var rectangle: Rectangle? = null

                    // Find which highlight ranges apply to this part of the paragraph,
                    // and apply them to the Text object. This means that each part could be
                    // styled in more than one way.
                    for (phr in paragraph.highlights) {
                        val highlight = phr.cause.highlight
                        if (phr.intersects(from, to)) {
                            highlight.style(text)

                            if (highlight is FillHighlight) {
                                if (rectangle == null) {
                                    rectangle = Rectangle(textBounds.width, textBounds.height)
                                    rectangle.styleClass.add("rectangle")
                                    rectangle.layoutX = text.layoutX
                                }
                                highlight.style(rectangle)
                            }
                        }
                    }
                    rectangle?.let { children.add(it) }
                    children.add(text)
                    x += textBounds.width
                }
            }
        }

        /**
         * Returns the column given the x coordinate (which is relative to the tedi area, not the node)
         */
        fun getColumn(x: Double): Int {
            val normX = virtualView.toContentX(x)

            var soFar = 0
            children.forEach { text ->
                if (text is Text) { // Ignore any Rectangles which may also be in the group.
                    if (normX <= text.layoutX) {
                        return soFar
                    }
                    val insertion = text.hitTestChar(normX, 1.0).getInsertionIndex()
                    if (insertion < text.text.length) {
                        return soFar + insertion
                    }
                    soFar += text.text.length
                }
            }
            return soFar
        }

        /**
         * Returns an X coordinate in pixels (relative to the TediArea), corresponding to a column.
         */
        fun xForColumn(column: Int): Double {
            //println("xForColumn $column")
            if (column == 0) return virtualView.fromContentX(0.0)

            var columnsEaten = 0
            children.forEach { text ->
                if (text is Text) { // Ignore any Rectangles which may also be in the group
                    columnsEaten += text.text.length

                    if (column == columnsEaten) {
                        // At the end of this piece of text.
                        //println("At the end of a segment ${text.boundsInParent.maxX} -> ${virtualView.fromContentX(text.boundsInParent.maxX)}")
                        return virtualView.fromContentX(text.boundsInParent.maxX)
                    } else if (column < columnsEaten) {
                        // In the middle of the text. Let's change the text, find the new bounds, then
                        // change it back
                        val oldText = text.text
                        try {
                            text.text = oldText.substring(0, column - columnsEaten + oldText.length)
                            //println("Middle of a segment $oldText ${text.boundsInParent.maxX} -> ${virtualView.fromContentX(text.boundsInParent.maxX)}")
                            return virtualView.fromContentX(text.boundsInParent.maxX)
                        } finally {
                            text.text = oldText
                        }

                    } else {
                        //println("Skipping ${text.text} eaten $columnsEaten")
                    }
                }
            }
            //println("At the end of loop ${boundsInLocal.maxX} -> ${virtualView.fromContentX(boundsInLocal.maxX)}")
            return virtualView.fromContentX(boundsInLocal.maxX)
        }
    }

    //--------------------------------------------------------------------------
    // Companion Object
    //--------------------------------------------------------------------------

    companion object {

        val TEXT_FILL = object : CssMetaData<TediArea, Paint>("-fx-text-fill",
                StyleConverter.getPaintConverter(), Color.BLACK) {
            override fun isSettable(n: TediArea) = !getStyleableProperty(n).isBound
            override fun getStyleableProperty(n: TediArea) = (n.skin as TediAreaSkin).textFillProperty
        }

        val HIGHLIGHT_FILL = object : CssMetaData<TediArea, Paint>("-fx-highlight-fill",
                StyleConverter.getPaintConverter(), Color.DODGERBLUE) {
            override fun isSettable(n: TediArea) = !getStyleableProperty(n).isBound
            override fun getStyleableProperty(n: TediArea) = (n.skin as TediAreaSkin).highlightFillProperty
        }

        val HIGHLIGHT_TEXT_FILL = object : CssMetaData<TediArea, Paint>("-fx-highlight-text-fill",
                StyleConverter.getPaintConverter(), Color.WHITE) {
            override fun isSettable(n: TediArea) = !getStyleableProperty(n).isBound
            override fun getStyleableProperty(n: TediArea) = (n.skin as TediAreaSkin).highlightTextFillProperty
        }

        val CURRENT_LINE_FILL = object : CssMetaData<TediArea, Paint>("-fx-current-line-fill",
                StyleConverter.getPaintConverter(), null) {
            override fun isSettable(n: TediArea) = !getStyleableProperty(n).isBound
            override fun getStyleableProperty(n: TediArea) = (n.skin as TediAreaSkin).currentLineFillProperty
        }

        val DISPLAY_CARET = object : CssMetaData<TediArea, Boolean>("-fx-display-caret",
                StyleConverter.getBooleanConverter(), java.lang.Boolean.TRUE) {
            override fun isSettable(n: TediArea) = !getStyleableProperty(n).isBound
            override fun getStyleableProperty(n: TediArea) = (n.skin as TediAreaSkin).displayCaretProperty
        }

        val STYLEABLES = listOf(
                TEXT_FILL, HIGHLIGHT_FILL, HIGHLIGHT_TEXT_FILL, CURRENT_LINE_FILL, DISPLAY_CARET)

        fun getClassCssMetaData() = STYLEABLES

    }
}
