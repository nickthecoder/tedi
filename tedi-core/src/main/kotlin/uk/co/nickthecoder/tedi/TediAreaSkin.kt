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
import javafx.beans.binding.IntegerBinding
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableIntegerValue
import javafx.collections.ListChangeListener.Change
import javafx.collections.ObservableList
import javafx.css.*
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.*
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import javafx.util.Duration
import uk.co.nickthecoder.tedi.ParagraphList.Paragraph
import uk.co.nickthecoder.tedi.javafx.BehaviorSkinBase

class TediAreaSkin(control: TediArea)

    : BehaviorSkinBase<TediArea, TediAreaBehavior>(control, TediAreaBehavior(control)) {

    /**
     * There is a 1:1 mapping between the groups children and [Paragraph]s.
     * i.e.
     *
     *     paragraphGroup.children[n] corresponds to ParagraphsList.paragraphs[n]
     *
     * For a paragraph without highlights, this will be a simple Text object.
     * For those with highlights, it will be a ??Group/TextFlow?? of Text objects.
     */
    private val paragraphGroup = Group()

    /**
     * A Region containing line numbers, to the left of the main content.
     */
    private val gutter = Gutter(control)

    /**
     * A simple BorderPane with left=[gutter], center=[contentView]
     */
    private val guttersAndContentView = BorderPane()

    /**
     * The main content responsible for displaying the text, the caret and the selection.
     *
     * Internal, as Gutter uses this to sync its top margin with the contentView's top margin to
     * ensure the line numbers line up with the main text.
     */
    internal val contentView = ContentView()
    // TODO Why internal?

    /**
     * Takes care of the contentView's borders, so that all ancestors don't need to bother.
     */
    private val insideGroup = Group()

    /**
     * A path, used to display the caret.
     */
    private val caretPath = Path()

    private val selectionHighlightGroup = Group()

    private val scrollPane = ScrollPane()

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
    private val textFill: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.BLACK) {
        override fun getBean() = this@TediAreaSkin
        override fun getName() = "textFill"
        override fun getCssMetaData() = TEXT_FILL
    }

    /**
     * The background behind the selected text
     */
    private val highlightFill: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.DODGERBLUE) {
        override fun getBean() = this@TediAreaSkin
        override fun getName() = "highlightFill"
        override fun getCssMetaData() = HIGHLIGHT_FILL
    }

    /**
     * The selected text's color
     */
    private val highlightTextFill: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.WHITE) {
        override fun getBean() = this@TediAreaSkin
        override fun getName() = "highlightTextFill"
        override fun getCssMetaData() = HIGHLIGHT_TEXT_FILL
    }

    private val displayCaret: StyleableBooleanProperty = object : StyleableBooleanProperty(true) {
        override fun getBean() = this@TediAreaSkin
        override fun getName() = "displayCaret"
        override fun getCssMetaData() = DISPLAY_CARET
    }

    // TODO What is this here for?
    private var caretPosition: ObservableIntegerValue = object : IntegerBinding() {
        init {
            bind(control.caretPositionProperty())
        }

        override fun computeValue(): Int {
            return control.caretPosition
        }
    }

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
                    control.disabledProperty(), control.editableProperty(), displayCaret)
        }

        override fun computeValue(): Boolean {
            return displayCaret.get() && control.isFocused &&
                    (control.caretPosition == control.anchor) &&
                    !control.isDisabled &&
                    control.isEditable
        }
    }

    private val caretAnimation = CaretAnimation(caretVisible, caretPath, caretPosition)

    /***************************************************************************
     *                                                                         *
     * init                                                                    *
     *                                                                         *
     **************************************************************************/

    init {

        // Initialize content
        with(scrollPane) {
            isFitToWidth = true
            isFitToHeight = true
            content = guttersAndContentView
            hvalueProperty().addListener { _, _, newValue -> skinnable.scrollLeft = newValue.toDouble() * getScrollLeftMax() }
            vvalueProperty().addListener { _, _, newValue -> skinnable.scrollTop = newValue.toDouble() * getScrollTopMax() }

            viewportBoundsProperty().addListener { _ ->
                if (scrollPane.viewportBounds != null) {
                    // ScrollPane creates a new Bounds instance for each
                    // layout pass, so we need to check if the width/height
                    // have really changed to avoid infinite layout requests.
                    val newViewportBounds = scrollPane.viewportBounds
                    if (oldViewportBounds.width != newViewportBounds.width || oldViewportBounds.height != newViewportBounds.height) {
                        oldViewportBounds = newViewportBounds
                        contentView.requestLayout()
                    }
                }
            }
        }
        children.add(scrollPane)

        // Create nodes for each paragraph.

        with(paragraphGroup) {
            skinnable.paragraphs.forEach { p ->
                children.add(createParagraphNode(p))
            }
            isManaged = false
        }

        // selection
        with(selectionHighlightGroup) {
            isManaged = false
        }

        // gutter
        gutter.updateLines()
        with(guttersAndContentView) {
            left = if (gutter.isVisible) gutter else null
            center = contentView
        }
        control.displayLineNumbersProperty().addListener { _, _, newValue ->
            if (newValue) {
                guttersAndContentView.left = gutter
            } else {
                guttersAndContentView.left = null
            }
        }

        // caretPath
        with(caretPath) {
            isManaged = false
            fillProperty().bind(textFill)
            strokeProperty().bind(textFill)
        }

        // tmpText
        tmpText.fontProperty().bind(control.fontProperty())

        // insideGroup
        with(insideGroup) {
            isManaged = false
            children.addAll(paragraphGroup, selectionHighlightGroup, caretPath)
        }

        // contentView
        contentView.children.add(insideGroup)

        // control

        control.scrollTopProperty().addListener { _, _, newValue ->
            scrollPane.vvalue = if (newValue.toDouble() < getScrollTopMax()) {
                newValue.toDouble() / getScrollTopMax()
            } else {
                1.0
            }
        }

        control.scrollLeftProperty().addListener { _, _, newValue ->
            scrollPane.hvalue = if (newValue.toDouble() < getScrollLeftMax()) {
                newValue.toDouble() / getScrollLeftMax()
            } else {
                1.0
            }
        }

        control.paragraphs.addListener { change: Change<out Paragraph> ->
            onParagraphsChange(change)
        }

        control.selectionProperty().addListener { _, _, _ ->
            onSelectionChanged()
        }

        caretPosition.addListener { _, _, _ -> onCaretMoved() }
        onCaretMoved()

        control.fontProperty().addListener { _, _, _ -> onFontChanged() }
        onFontChanged()
    }


    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a simple Text object if the paragraph has no highlights,
     * otherwise it creates a set of Text objects, each with their own styles.
     */
    private fun createParagraphNode(paragraph: Paragraph): Node {

        fun createText(str: String): Text {
            return Text(str).apply {
                styleClass.add("text")
                textOrigin = VPos.TOP
                wrappingWidth = 0.0
                isManaged = false
                font = skinnable.font
                fill = textFill.get()
            }
        }

        if (paragraph.highlights.isEmpty()) {
            return createText(paragraph.text)

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

            // The paragraph will be made up of a Group containing Text and Rectangles (for background colours).
            val group = Group()
            group.isManaged = false

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
                rectangle?.let { group.children.add(it) }
                group.children.add(text)
                x += textBounds.width
            }

            return group
        }
    }

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
                fillProperty().bind(highlightTextFill)
            }

            val bounds = text.boundsInLocal
            val rectangle = Rectangle(bounds.width, bounds.height).apply {
                layoutX = text.layoutX
                layoutY = text.layoutY
                fillProperty().bind(highlightFill)
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
        contentView.requestLayout() // TODO Remove this?
    }

    /**
     * Called whenever a [Paragraph] is changed. The change could be an insertion/deletion of text,
     * or just a highlight change (in which case the Paragraph's text will be the same).
     */
    fun onParagraphsChange(change: Change<out Paragraph>) {
        while (change.next()) {
            if (change.wasAdded()) {
                for (i in change.from..change.to - 1) {
                    paragraphGroup.children.add(i, createParagraphNode(change.list[i]))
                }
            }
            if (change.wasRemoved()) {
                val from = change.from
                for (n in 1..change.removedSize) {
                    paragraphGroup.children.removeAt(from)
                }
            }
            if (change.wasUpdated()) {
                for (i in change.from..change.to - 1) {
                    rebuildParagraph(i)
                }
            }
            contentView.requestLayout()
        }
    }

    fun rebuildParagraph(i: Int) {
        val child = paragraphGroup.children[i]
        val paragraph = skinnable.paragraphs[i]
        // A simple text change without highlights before and after?
        if (child is Text && paragraph.highlights.isEmpty()) {
            // We can reuse the existing Text object
            child.text = paragraph.text
            child.font = skinnable.font
        } else {
            paragraphGroup.children[i] = createParagraphNode(paragraph)
        }
    }

    fun onCaretMoved() {
        targetCaretX = -1.0

        val (line, column) = skinnable.lineColumnForPosition(skinnable.caretPosition)
        caretPath.layoutY = line * lineHeight()
        tmpText.text = skinnable.paragraphs[line].charSequence.substring(0, column)
        caretPath.layoutX = tmpText.boundsInLocal.width

        // TODO Should the view scroll even when not focused?
        //if (skinnable.isFocused) {
        scrollCaretToVisible()
        //}
    }

    fun onFontChanged() {
        cachedLineHeight = 0.0
        caretPath.elements.clear()
        caretPath.elements.add(MoveTo(0.0, 0.0))
        caretPath.elements.add(LineTo(0.0, lineHeight()))
        caretPath.fillProperty().bind(textFill)
        caretPath.strokeWidth = Math.min(1.0, lineHeight() / 15.0)
        for (i in 0..skinnable.paragraphs.size - 1) {
            rebuildParagraph(i)
        }
        contentView.requestLayout()
    }

    override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
        scrollPane.resizeRelocate(contentX, contentY, contentWidth, contentHeight)
    }

    fun positionForPoint(x: Double, y: Double): Int {
        val point = Point2D(0.0, 0.0)
        val contentViewBounds = contentView.localToScene(point)
        val controlBounds = skinnable.localToScene(point)
        return positionForContentPoint(
                x + controlBounds.x - contentViewBounds.x,
                y + controlBounds.y - contentViewBounds.y)
    }

    fun positionForContentPoint(x: Double, y: Double): Int {
        val normX = x - insideGroup.layoutX
        val normY = y - insideGroup.layoutY
        if (normY < 0) return 0 // Beyond the top
        val line = (normY / lineHeight()).toInt()

        if (line >= paragraphGroup.children.size) {
            return skinnable.length // Beyond the bottom. End of document.
        }

        val node = paragraphGroup.children[line]
        if (node is Text) {
            return skinnable.positionOfLine(line) + node.hitTestChar(normX, normY).getInsertionIndex()
        } else if (node is Group) {
            var soFar = skinnable.positionOfLine(line)
            node.children.forEach { text ->
                if (text is Text) { // Ignore any Rectangles which may also be in the group.
                    if (normX < text.layoutX) {
                        return soFar
                    }
                    val insertion = text.hitTestChar(normX, normY - node.layoutY).getInsertionIndex()
                    if (insertion < text.text.length) {
                        return soFar + insertion
                    }
                    soFar += text.text.length
                }
            }
            return soFar
        } else {
            throw IllegalStateException("Unexpected Node type : ${node::class.java.simpleName}")
        }
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

    private fun getScrollTopMax(): Double {
        return Math.max(0.0, contentView.height - scrollPane.viewportBounds.height)
    }

    private fun getScrollLeftMax(): Double {
        return Math.max(0.0, contentView.width - scrollPane.viewportBounds.width)
    }

    private fun scrollCaretToVisible() {
        val textArea = skinnable
        val bounds = caretPath.layoutBounds
        val x = bounds.minX - textArea.scrollLeft + caretPath.layoutX
        val y = bounds.minY - textArea.scrollTop + caretPath.layoutY
        val w = bounds.width
        val h = bounds.height

        if (w > 0 && h > 0) {
            scrollBoundsToVisible(Rectangle2D(x, y, w, h))
        }
    }

    private fun scrollBoundsToVisible(bounds: Rectangle2D) {
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
    }

    /**
     * Move the caret up (or down if n < 1) n lines, keeping the caret in roughly the same X coordinate.
     * The desired X coordinate is stored in [targetCaretX], which is reset whenever the selection changes.
     */
    private fun changeLine(n: Int, select: Boolean) {
        val line = skinnable.lineForPosition(skinnable.caretPosition)

        val requiredX = if (targetCaretX < 0) caretPath.layoutX else targetCaretX

        val requiredLine = clamp(0, line + n, skinnable.lineCount - 1)
        val node = paragraphGroup.children[requiredLine]

        var columnIndex = 0
        if (node is Text) {
            val hit = node.hitTestChar(requiredX, node.layoutY)
            columnIndex = hit.getInsertionIndex()
        } else if (node is Group) {
            for (child in node.children) {
                if (child is Text) {
                    val hit = child.hitTestChar(requiredX, 0.0)
                    val i = hit.getInsertionIndex()
                    if (i == 0) break
                    columnIndex += i
                }
            }
        }

        val newPosition = skinnable.positionOfLine(requiredLine, 0) + columnIndex

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

    /**
     * Returns the number of lines to scroll up/down by.
     */
    private fun pageSizeInLines(): Int {
        val result = scrollPane.viewportBounds.height / lineHeight()
        return result.toInt() - 1
    }

    fun previousPage(select: Boolean) {
        val lines = pageSizeInLines()
        // This calculation is a little off, and the caret tends to wander upwards as you scroll up
        scrollPane.vvalue -= lines * lineHeight() / contentView.height
        changeLine(-lines, select)
    }

    fun nextPage(select: Boolean) {
        val lines = pageSizeInLines()
        // This calculation is a little off, and the caret tends to wander downwards as you scroll down.
        scrollPane.vvalue += lines * lineHeight() / contentView.height
        changeLine(lines, select)
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

    /***************************************************************************
     *                                                                         *
     * CaretAnimation class                                                    *
     *                                                                         *
     **************************************************************************/
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

    /***************************************************************************
     *                                                                         *
     * Content View class                                                      *
     *                                                                         *
     **************************************************************************/
    inner class ContentView : Region() {

        init {
            styleClass.add("content")

            addEventHandler(MouseEvent.MOUSE_PRESSED) { event ->
                behavior.mousePressed(event)
                event.consume()
            }

            addEventHandler(MouseEvent.MOUSE_RELEASED) { event ->
                behavior.mouseReleased(event)
                event.consume()
            }

            addEventHandler(MouseEvent.MOUSE_DRAGGED) { event ->
                behavior.mouseDragged(event)
                event.consume()
            }

        }

        // TODO do we can children to be public?
        public override fun getChildren(): ObservableList<Node> {
            return super.getChildren()
        }

        override fun getContentBias() = Orientation.HORIZONTAL

        override fun computePrefWidth(height: Double): Double {
            var maxWidth = 0.0
            paragraphGroup.children.forEach { text ->
                maxWidth = Math.max(maxWidth, text.prefWidth(height))
            }
            return maxWidth + snappedLeftInset() + snappedRightInset()
        }

        override fun computePrefHeight(width: Double): Double {
            var total = 0.0
            paragraphGroup.children.forEach { text ->
                total += text.prefHeight(width)
            }
            return total + snappedTopInset() + snappedBottomInset()
        }

        public override fun layoutChildren() {
            val width = width

            // insideGroup
            insideGroup.layoutX = snappedLeftInset()
            insideGroup.layoutY = snappedTopInset()

            var textY = 0.0
            val lineHeight = lineHeight()
            paragraphGroup.children.forEach { text ->
                text.layoutY = textY
                textY += lineHeight
            }

            // Fit to width/height only if smaller than viewport.
            // That is, grow to fit but don't shrink to fit.
            val viewportBounds = scrollPane.viewportBounds
            val wasFitToWidth = scrollPane.isFitToWidth
            val wasFitToHeight = scrollPane.isFitToHeight
            val setFitToWidth = computePrefWidth(-1.0) <= viewportBounds.width
            val setFitToHeight = computePrefHeight(width) <= viewportBounds.height

            if (wasFitToWidth != setFitToWidth || wasFitToHeight != setFitToHeight) {
                Platform.runLater {
                    scrollPane.isFitToWidth = setFitToWidth
                    scrollPane.isFitToHeight = setFitToHeight
                    parent.requestLayout()
                }
            }
        }
    }

    override fun getCssMetaData(): List<CssMetaData<out Styleable, *>> {
        return getClassCssMetaData()
    }

    /***************************************************************************
     *                                                                         *
     * Companion Object                                                        *
     *                                                                         *
     **************************************************************************/
    companion object {

        private val TEXT_FILL = object : CssMetaData<TediArea, Paint>("-fx-charSequence-fill",
                StyleConverter.getPaintConverter(), Color.BLACK) {
            override fun isSettable(n: TediArea) = !getStyleableProperty(n).isBound
            override fun getStyleableProperty(n: TediArea) = (n.skin as TediAreaSkin).textFill
        }

        private val HIGHLIGHT_FILL = object : CssMetaData<TediArea, Paint>("-fx-highlight-fill",
                StyleConverter.getPaintConverter(), Color.DODGERBLUE) {
            override fun isSettable(n: TediArea) = !getStyleableProperty(n).isBound
            override fun getStyleableProperty(n: TediArea) = (n.skin as TediAreaSkin).highlightFill
        }

        private val HIGHLIGHT_TEXT_FILL = object : CssMetaData<TediArea, Paint>("-fx-highlight-charSequence-fill",
                StyleConverter.getPaintConverter(), Color.WHITE) {
            override fun isSettable(n: TediArea) = !getStyleableProperty(n).isBound
            override fun getStyleableProperty(n: TediArea) = (n.skin as TediAreaSkin).highlightTextFill
        }

        private val DISPLAY_CARET = object : CssMetaData<TediArea, Boolean>("-fx-display-caret",
                StyleConverter.getBooleanConverter(), java.lang.Boolean.TRUE) {
            override fun isSettable(n: TediArea) = !getStyleableProperty(n).isBound
            override fun getStyleableProperty(n: TediArea) = (n.skin as TediAreaSkin).displayCaret
        }

        private val STYLEABLES = listOf(
                TEXT_FILL, HIGHLIGHT_FILL, HIGHLIGHT_TEXT_FILL, DISPLAY_CARET)

        fun getClassCssMetaData() = STYLEABLES

    }
}
