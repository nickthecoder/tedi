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

import com.sun.javafx.scene.text.TextLayout
import com.sun.javafx.tk.Toolkit
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.DoubleBinding
import javafx.beans.binding.IntegerBinding
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableIntegerValue
import javafx.collections.ObservableList
import javafx.css.*
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.*
import javafx.scene.AccessibleAttribute
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextInputControl
import javafx.scene.input.InputMethodEvent
import javafx.scene.input.InputMethodHighlight
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.*
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextBoundsType
import javafx.util.Duration
import uk.co.nickthecoder.tedi.javafx.BehaviorSkinBase
import java.lang.ref.WeakReference
import java.util.*

open class TediAreaSkin(val control: TediArea)

    : BehaviorSkinBase<TediArea, TediAreaBehavior>(control, TediAreaBehavior(control)) {

    private val paragraphNode = Text()
    private val gutter = Gutter(this)

    private val guttersAndContentView = BorderPane()
    internal val contentView = ContentView()

    /**
     * A path, provided by the textNode, which represents the caret.
     * I assume this has to be updated whenever the caretPosition
     * changes. Perhaps more frequently (including text changes),
     * but I'm not sure.
     */
    protected val caretPath = Path()

    private val selectionHighlightGroup = Group()

    private val scrollPane = ScrollPane()

    private var oldViewportBounds: Bounds? = null

    private val scrollDirection: VerticalDirection? = null

    private val characterBoundingPath = Path()

    private val scrollSelectionTimeline = Timeline()

    private val scrollSelectionHandler = EventHandler<ActionEvent> {
        when (scrollDirection) {
            VerticalDirection.UP -> {
            }// TODO Get previous offset

            VerticalDirection.DOWN -> {
            }// TODO Get next offset
        }
    }

    /**
     * Remembers horizontal position when traversing up / down.
     */
    internal var targetCaretX = -1.0

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    private val forwardBias = SimpleBooleanProperty(this, "forwardBias", true)

    /**
     * The fill to use for the text under normal conditions
     */
    protected val textFill: ObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.BLACK) {

        override fun getBean(): Any {
            return this@TediAreaSkin
        }

        override fun getName(): String {
            return "textFill"
        }

        override fun getCssMetaData(): CssMetaData<TediArea, Paint> {
            return StyleableProperties.TEXT_FILL
        }
    }

    /**
     * The fill to use for the text when highlighted.
     */
    protected val highlightFill: ObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.DODGERBLUE) {
        override fun invalidated() {
            updateHighlightFill()
        }

        override fun getBean(): Any {
            return this@TediAreaSkin
        }

        override fun getName(): String {
            return "highlightFill"
        }

        override fun getCssMetaData(): CssMetaData<TediArea, Paint> {
            return StyleableProperties.HIGHLIGHT_FILL
        }
    }

    protected val highlightTextFill: ObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.WHITE) {
        override fun invalidated() {
            updateHighlightTextFill()
        }

        override fun getBean(): Any {
            return this@TediAreaSkin
        }

        override fun getName(): String {
            return "highlightTextFill"
        }

        override fun getCssMetaData(): CssMetaData<TediArea, Paint> {
            return StyleableProperties.HIGHLIGHT_TEXT_FILL
        }
    }

    protected val displayCaret: BooleanProperty = object : StyleableBooleanProperty(true) {
        override fun getBean(): Any {
            return this@TediAreaSkin
        }

        override fun getName(): String {
            return "displayCaret"
        }

        override fun getCssMetaData(): CssMetaData<TediArea, Boolean> {
            return StyleableProperties.DISPLAY_CARET
        }
    }

    private var caretPosition: ObservableIntegerValue = object : IntegerBinding() {
        init {
            bind(control.caretPositionProperty())
        }

        override fun computeValue(): Int {
            return control.caretPosition
        }
    }

    private val blink = SimpleBooleanProperty(this, "blink", true)

    private val caretBlinking = CaretBlinking(blink)

    /**
     * The caret is visible when the text box is focused AND when the selection
     * is empty. If the selection is non empty or the text box is not focused
     * then we don't want to show the caret. Also, we show the caret while
     * performing some operations such as most key strokes. In that case we
     * simply toggle its opacity.
     */
    protected var caretVisible: ObservableBooleanValue = object : BooleanBinding() {
        init {
            bind(control.focusedProperty(), control.anchorProperty(), control.caretPositionProperty(),
                    control.disabledProperty(), control.editableProperty(), displayCaret, blink)
        }

        override fun computeValue(): Boolean {
            return !blink.get() && displayCaret.get() && control.isFocused() &&
                    (control.caretPosition == control.anchor) &&
                    !control.isDisabled() &&
                    control.isEditable
        }
    }

    /***************************************************************************
     *                                                                         *
     * init                                                                    *
     *                                                                         *
     **************************************************************************/

    init {

        // Add initial text content.
        createParagraphNode()

        if (control.onInputMethodTextChanged == null) {
            control.setOnInputMethodTextChanged({ event -> handleInputMethodEvent(event) })
        }

        caretPosition.addListener { _, oldValue, newValue ->
            targetCaretX = -1.0
            if (newValue.toInt() > oldValue.toInt()) {
                setForwardBias(true)
            }
        }

        forwardBias.addListener({ _ ->
            if (control.width > 0) {
                updateTextNodeCaretPos(control.caretPosition)
            }
        })

        // Initialize content
        scrollPane.isFitToWidth = true
        scrollPane.isFitToHeight = true
        scrollPane.content = guttersAndContentView
        children.add(scrollPane)

        // Add selection
        selectionHighlightGroup.isManaged = false
        selectionHighlightGroup.isVisible = false
        contentView.children.add(selectionHighlightGroup)

        // Add content view
        paragraphNode.isManaged = false
        contentView.children.addAll(paragraphNode)


        // gutter
        guttersAndContentView.left = if (gutter.isVisible) gutter else null
        guttersAndContentView.center = contentView

        control.displayLineNumbersProperty().addListener { _, _, _ ->
            updateGutters()
        }

        // Add caret
        caretPath.isManaged = false
        caretPath.strokeWidth = 1.0
        caretPath.fillProperty().bind(textFill)
        caretPath.strokeProperty().bind(textFill)
        // modifying visibility of the caret forces a layout-pass (RT-32373), so
        // instead we modify the opacity.
        caretPath.opacityProperty().bind(object : DoubleBinding() {
            init {
                bind(caretVisible)
            }

            override fun computeValue(): Double {
                return if (caretVisible.get()) 1.0 else 0.0
            }
        })
        contentView.children.add(caretPath)

        scrollPane.hvalueProperty().addListener { _, _, newValue -> skinnable.scrollLeft = newValue.toDouble() * getScrollLeftMax() }
        scrollPane.vvalueProperty().addListener { _, _, newValue -> skinnable.scrollTop = newValue.toDouble() * getScrollTopMax() }

        // Initialize the scroll selection timeline
        scrollSelectionTimeline.cycleCount = Timeline.INDEFINITE
        val scrollSelectionFrames = scrollSelectionTimeline.keyFrames
        scrollSelectionFrames.clear()
        scrollSelectionFrames.add(KeyFrame(Duration.millis(350.0), scrollSelectionHandler))

        control.selectionProperty().addListener { _, _, _ ->
            // Why do we need two calls here? (from original)
            control.requestLayout()
            contentView.requestLayout()
        }

        scrollPane.viewportBoundsProperty().addListener { _ ->
            if (scrollPane.viewportBounds != null) {
                // ScrollPane creates a new Bounds instance for each
                // layout pass, so we need to check if the width/height
                // have really changed to avoid infinite layout requests.
                val newViewportBounds = scrollPane.viewportBounds
                if (oldViewportBounds == null ||
                        oldViewportBounds?.width != newViewportBounds.width ||
                        oldViewportBounds?.height != newViewportBounds.height) {

                    oldViewportBounds = newViewportBounds
                    contentView.requestLayout()
                }
            }
        }

        control.scrollTopProperty().addListener { _, _, newValue ->
            val vValue = if (newValue.toDouble() < getScrollTopMax())
                newValue.toDouble() / getScrollTopMax()
            else
                1.0
            scrollPane.vvalue = vValue
        }

        control.scrollLeftProperty().addListener { _, _, newValue ->
            val hValue = if (newValue.toDouble() < getScrollLeftMax())
                newValue.toDouble() / getScrollLeftMax()
            else
                1.0
            scrollPane.hvalue = hValue
        }

        control.textProperty().addListener { _ ->
            paragraphNode.text = control.textProperty().valueSafe
            contentView.requestLayout()
        }

        updateHighlightFill()
        if (control.isFocused) setCaretAnimating(true)

    }

    override fun dispose() {
        // TODO Unregister listeners on text editor, paragraph list
        throw UnsupportedOperationException()
    }

    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    private fun updateGutters() {
        if (control.displayLineNumbers) {
            guttersAndContentView.left = gutter
        } else {
            guttersAndContentView.left = null
        }
    }

    override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
        scrollPane.resizeRelocate(contentX, contentY, contentWidth, contentHeight)
    }

    private fun createParagraphNode() {
        with(paragraphNode) {
            text = control.text
            textOrigin = VPos.TOP
            wrappingWidth = 0.0
            isManaged = false
            styleClass.add("text")

            fontProperty().bind(control.fontProperty())
            fillProperty().bind(textFill)
        }

    }

    public override fun computeBaselineOffset(topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        return getAscent(skinnable.font, paragraphNode.boundsType) + contentView.snappedTopInset() + control.snappedTopInset()
    }

    fun getHitInformation(x: Double, y: Double): HitInformation = paragraphNode.hitTestChar(x, y)

    fun positionCaret(hit: HitInformation, select: Boolean, extendSelection: Boolean) {

        var pos = hit.getInsertionIndex()
        val isNewLine = pos > 0 &&
                pos <= skinnable.length &&
                skinnable.text.codePointAt(pos - 1) == 0x0a

        // special handling for a new line
        if (!hit.isLeading && isNewLine) {
            hit.isLeading = true
            pos -= 1
        }

        if (select) {
            if (extendSelection) {
                skinnable.extendSelection(pos)
            } else {
                skinnable.selectPositionCaret(pos)
            }
        } else {
            skinnable.positionCaret(pos)
        }

        setForwardBias(hit.isLeading)
    }

    private fun getScrollTopMax(): Double {
        return Math.max(0.0, contentView.height - scrollPane.viewportBounds.height)
    }

    private fun getScrollLeftMax(): Double {
        return Math.max(0.0, contentView.width - scrollPane.viewportBounds.width)
    }

    fun getCharacterBounds(index: Int): Rectangle2D {
        val textArea = skinnable

        characterBoundingPath.elements.clear()
        characterBoundingPath.elements.addAll(*paragraphNode.impl_getRangeShape(index, index + 1))
        characterBoundingPath.layoutX = paragraphNode.layoutX
        characterBoundingPath.layoutY = paragraphNode.layoutY

        val bounds = characterBoundingPath.boundsInLocal

        val x = bounds.minX + paragraphNode.layoutX - textArea.scrollLeft
        val y = bounds.minY + paragraphNode.layoutY - textArea.scrollTop

        // Sometimes the bounds is empty, in which case we must ignore the width/height
        val width = if (bounds.isEmpty) 0.0 else bounds.width
        val height = if (bounds.isEmpty) 0.0 else bounds.height

        return Rectangle2D(x, y, width, height)
    }

    private fun scrollCaretToVisible() {
        val textArea = skinnable
        val bounds = caretPath.layoutBounds
        val x = bounds.minX - textArea.scrollLeft
        val y = bounds.minY - textArea.scrollTop
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

    protected fun updateHighlightTextFill() {
        for (node in selectionHighlightGroup.children) {
            val selectionHighlightPath = node as Path
            selectionHighlightPath.fill = highlightFill.get()
        }
    }

    protected fun updateHighlightFill() {
        for (node in selectionHighlightGroup.children) {
            val selectionHighlightPath = node as Path
            selectionHighlightPath.fill = highlightFill.get()
        }
    }

    private fun changeLine(n: Int, select: Boolean) {
        val lineColumn = control.lineColumnFor(control.caretPosition)
        val newPosition = control.positionFor(lineColumn.first + n, lineColumn.second)
        if (select) {
            control.selectRange(control.anchor, newPosition)
        } else {
            control.selectRange(newPosition, newPosition)
        }
    }

    fun previousLine(select: Boolean) {
        changeLine(-1, select)
    }

    fun nextLine(select: Boolean) {
        changeLine(1, select)
    }

    /**
     * Returns a rough calculation of the line height
     */
    private fun lineHeight(): Double {
        return paragraphNode.prefHeight(0.0) / control.lineCount
    }

    /**
     * Returns the number of lines to scroll up/down by.
     * This is a rough calculation, based on the size of content, and the number of lines it contains.
     * When there are fewer lines than can fit within the visible viewport, then the returned value is wrong,
     * but as it is only used for scrolling, it doesn't matter!
     */
    private fun pageSizeInLines(): Int {
        val result = scrollPane.height / (paragraphNode.prefHeight(0.0) / control.lineCount)
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
        val lineColumn = control.lineColumnFor(control.caretPosition)
        val newPosition = control.positionFor(lineColumn.first, 0)
        if (select) {
            control.selectRange(control.anchor, newPosition)
        } else {
            control.selectRange(newPosition, newPosition)
        }
    }

    fun lineEnd(select: Boolean) {
        val lineColumn = control.lineColumnFor(control.caretPosition)
        val newPosition = control.positionFor(lineColumn.first, Int.MAX_VALUE)
        if (select) {
            control.selectRange(control.anchor, newPosition)
        } else {
            control.selectRange(newPosition, newPosition)
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


    private fun updateTextNodeCaretPos(pos: Int) {
        if (isForwardBias()) {
            paragraphNode.impl_caretPosition = pos
        } else {
            paragraphNode.impl_caretPosition = pos - 1
        }
        paragraphNode.impl_caretBiasProperty().set(isForwardBias())
    }

    protected fun getUnderlineShape(start: Int, end: Int): Array<PathElement>? {
        var pStart = 0
        val pEnd = pStart + paragraphNode.textProperty().valueSafe.length
        if (pEnd >= start) {
            return paragraphNode.impl_getUnderlineShape(start - pStart, end - pStart)
        }
        pStart = pEnd + 1

        return null
    }

    protected fun getRangeShape(start: Int, end: Int): Array<out PathElement>? {
        return paragraphNode.impl_getRangeShape(start, end)
    }

    protected fun addHighlight(nodes: List<Node>, start: Int) {

        for (node in nodes) {
            node.layoutX = paragraphNode.layoutX
            node.layoutY = paragraphNode.layoutY
        }
        contentView.children.addAll(nodes)
    }

    protected fun removeHighlight(nodes: List<Node>) {
        contentView.children.removeAll(nodes)
    }

    /**
     * Use this implementation instead of the one provided on TextInputControl
     * Simply calls into TextInputControl.deletePrevious/NextChar and responds appropriately
     * based on the return value.
     */
    fun deleteChar(previous: Boolean) {
        val shouldBeep = if (previous)
            !skinnable.deletePreviousChar()
        else
            !skinnable.deleteNextChar()
    }

    override fun queryAccessibleAttribute(attribute: AccessibleAttribute?, vararg parameters: Any): Any {
        when (attribute) {
            AccessibleAttribute.LINE_FOR_OFFSET, AccessibleAttribute.LINE_START, AccessibleAttribute.LINE_END, AccessibleAttribute.BOUNDS_FOR_RANGE, AccessibleAttribute.OFFSET_AT_POINT -> {
                return paragraphNode.queryAccessibleAttribute(attribute, *parameters)
            }
            else -> return super.queryAccessibleAttribute(attribute, *parameters)
        }
    }

    fun setForwardBias(isLeading: Boolean) {
        forwardBias.set(isLeading)
    }

    fun isForwardBias(): Boolean {
        return forwardBias.get()
    }

    // Start/Length of the text under input method composition
    private var imstart: Int = 0
    private var imlength: Int = 0
    // Holds concrete attributes for the composition runs
    private val imattrs = ArrayList<Shape>()

    protected fun handleInputMethodEvent(event: InputMethodEvent) {
        val textInput = skinnable
        if (textInput.isEditable && !textInput.textProperty().isBound && !textInput.isDisabled) {

            // remove previous input method text (if any) or selected text
            if (imlength != 0) {
                removeHighlight(imattrs)
                imattrs.clear()
                textInput.selectRange(imstart, imstart + imlength)
            }

            // Insert committed text
            if (event.committed.isNotEmpty()) {
                val committed = event.committed
                textInput.replaceText(textInput.selection, committed)
            }

            // Replace composed text
            imstart = textInput.selection.start
            val composed = StringBuilder()
            for (run in event.composed) {
                composed.append(run.text)
            }
            textInput.replaceText(textInput.selection, composed.toString())
            imlength = composed.length
            if (imlength != 0) {
                var pos = imstart
                for (run in event.composed) {
                    val endPos = pos + run.text.length
                    createInputMethodAttributes(run.highlight, pos, endPos)
                    pos = endPos
                }
                addHighlight(imattrs, imstart)

                // Set caret position in composed text
                val caretPos = event.caretPosition
                if (caretPos >= 0 && caretPos < imlength) {
                    textInput.selectRange(imstart + caretPos, imstart + caretPos)
                }
            }
        }
    }

    private fun createInputMethodAttributes(highlight: InputMethodHighlight, start: Int, end: Int) {
        var minX = 0.0
        var maxX = 0.0
        var minY = 0.0
        var maxY = 0.0

        val elements = getUnderlineShape(start, end) ?: return

        for (i in elements.indices) {
            val pe = elements[i]
            if (pe is MoveTo) {
                maxX = pe.x
                minX = maxX
                maxY = pe.y
                minY = maxY
            } else if (pe is LineTo) {
                minX = if (minX < pe.x) minX else pe.x
                maxX = if (maxX > pe.x) maxX else pe.x
                minY = if (minY < pe.y) minY else pe.y
                maxY = if (maxY > pe.y) maxY else pe.y
            } else if (pe is HLineTo) {
                minX = if (minX < pe.x) minX else pe.x
                maxX = if (maxX > pe.x) maxX else pe.x
            } else if (pe is VLineTo) {
                minY = if (minY < pe.y) minY else pe.y
                maxY = if (maxY > pe.y) maxY else pe.y
            }
            // Don't assume that shapes are ended with ClosePath.
            if (pe is ClosePath ||
                    i == elements.size - 1 ||
                    i < elements.size - 1 && elements[i + 1] is MoveTo) {
                // Now, create the attribute.
                var attr: Shape? = null
                if (highlight == InputMethodHighlight.SELECTED_RAW) {
                    // blue background
                    attr = Path()
                    attr.elements.addAll(getRangeShape(start, end)!!)
                    attr.fill = Color.BLUE
                    attr.opacity = 0.3
                } else if (highlight == InputMethodHighlight.UNSELECTED_RAW) {
                    // dash underline.
                    attr = Line(minX + 2, maxY + 1, maxX - 2, maxY + 1)
                    attr.stroke = textFill.get()
                    attr.strokeWidth = maxY - minY
                    val dashArray = attr.strokeDashArray
                    dashArray.add(java.lang.Double.valueOf(2.0))
                    dashArray.add(java.lang.Double.valueOf(2.0))
                } else if (highlight == InputMethodHighlight.SELECTED_CONVERTED) {
                    // thick underline.
                    attr = Line(minX + 2, maxY + 1, maxX - 2, maxY + 1)
                    attr.stroke = textFill.get()
                    attr.strokeWidth = (maxY - minY) * 3
                } else if (highlight == InputMethodHighlight.UNSELECTED_CONVERTED) {
                    // single underline.
                    attr = Line(minX + 2, maxY + 1, maxX - 2, maxY + 1)
                    attr.stroke = textFill.get()
                    attr.strokeWidth = maxY - minY
                }

                if (attr != null) {
                    attr.isManaged = false
                    imattrs.add(attr)
                }
            }
        }
    }

    fun setCaretAnimating(value: Boolean) {
        if (value) {
            caretBlinking.start()
        } else {
            caretBlinking.stop()
            blink.set(true)
        }
    }

    /***************************************************************************
     *                                                                         *
     * Caret Blinking class                                                    *
     *                                                                         *
     **************************************************************************/
    private class CaretBlinking(blinkProperty: BooleanProperty) {

        private val caretTimeline = Timeline()

        private val blinkPropertyRef = WeakReference(blinkProperty)

        init {
            caretTimeline.cycleCount = Timeline.INDEFINITE
            caretTimeline.keyFrames.addAll(
                    KeyFrame(Duration.ZERO, EventHandler<ActionEvent> { setBlink(false) }),
                    KeyFrame(Duration.seconds(.5), EventHandler<ActionEvent> { setBlink(true) }),
                    KeyFrame(Duration.seconds(1.0)))
        }

        fun start() {
            caretTimeline.play()
        }

        fun stop() {
            caretTimeline.stop()
        }

        private fun setBlink(value: Boolean) {
            val blinkProperty = blinkPropertyRef.get()
            if (blinkProperty == null) {
                caretTimeline.stop()
                return
            }

            blinkProperty.set(value)
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

        public override fun getChildren(): ObservableList<Node> {
            return super.getChildren()
        }

        override fun getContentBias(): Orientation {
            return Orientation.HORIZONTAL
        }

        override fun computePrefWidth(height: Double): Double {
            return paragraphNode.prefWidth(height) + snappedLeftInset() + snappedRightInset()
        }

        override fun computePrefHeight(width: Double): Double {
            return paragraphNode.prefHeight(width) + snappedTopInset() + snappedBottomInset()
        }

        public override fun layoutChildren() {
            val tediArea = skinnable
            val width = width

            // Lay out paragraph
            paragraphNode.layoutX = snappedLeftInset()
            paragraphNode.layoutY = snappedTopInset()

            // Update the selection
            val selection = tediArea.selection
            val oldCaretBounds = caretPath.boundsInParent

            selectionHighlightGroup.children.clear()

            val caretPos = tediArea.caretPosition

            // Position caret
            updateTextNodeCaretPos(caretPos)
            caretPath.elements.clear()
            caretPath.elements.addAll(*paragraphNode.impl_caretShape)

            caretPath.layoutX = paragraphNode.layoutX

            // TODO: Remove this temporary workaround for RT-27533
            //paragraphNode.layoutX = 2 * paragraphNode.layoutX - paragraphNode.boundsInParent.minX

            caretPath.layoutY = paragraphNode.layoutY
            if (oldCaretBounds == null || oldCaretBounds != caretPath.boundsInParent) {
                scrollCaretToVisible()
            }

            // Update selection fg and bg
            val start = selection.start
            val end = selection.end
            var i = 0

            val paragraphLength = paragraphNode.text.length + 1
            if (end > start && start < paragraphLength) {
                paragraphNode.impl_selectionStart = start
                paragraphNode.impl_selectionEnd = Math.min(end, paragraphLength)

                val selectionHighlightPath = Path()
                selectionHighlightPath.isManaged = false
                selectionHighlightPath.stroke = null
                paragraphNode.impl_selectionShape?.let {
                    selectionHighlightPath.elements.addAll(*it)
                }
                selectionHighlightGroup.children.add(selectionHighlightPath)
                selectionHighlightGroup.isVisible = true
                selectionHighlightPath.layoutX = paragraphNode.layoutX
                selectionHighlightPath.layoutY = paragraphNode.layoutY
                updateHighlightFill()
            } else {
                paragraphNode.impl_selectionStart = -1
                paragraphNode.impl_selectionEnd = -1
                selectionHighlightGroup.isVisible = false
            }

            // TODO What is this for? Do we need it?
            /*
            if (scrollPane.prefViewportWidth == 0.0 || scrollPane.prefViewportHeight == 0.0) {
                if (parent != null && scrollPane.prefViewportWidth > 0 || scrollPane.prefViewportHeight > 0) {
                    // Force layout of viewRect in ScrollPaneSkin
                    parent.requestLayout()
                }
            }
            */

            // Fit to width/height only if smaller than viewport.
            // That is, grow to fit but don't shrink to fit.
            val viewportBounds = scrollPane.viewportBounds
            val wasFitToWidth = scrollPane.isFitToWidth
            val wasFitToHeight = scrollPane.isFitToHeight
            val setFitToWidth = computePrefWidth(-1.0) <= viewportBounds.width
            val setFitToHeight = computePrefHeight(width) <= viewportBounds.height

            if (wasFitToWidth != setFitToWidth || wasFitToHeight != setFitToHeight) {
                // println("Changing fit from $wasFitToWidth , $wasFitToHeight to  $setFitToWidth , $setFitToHeight ")
                Platform.runLater {
                    scrollPane.isFitToWidth = setFitToWidth
                    scrollPane.isFitToHeight = setFitToHeight
                    parent.requestLayout()
                }
            }
        }
    }

    /***************************************************************************
     *                                                                         *
     * StyleableProperties object                                              *
     *                                                                         *
     **************************************************************************/
    private object StyleableProperties {

        val TEXT_FILL = object : CssMetaData<TediArea, Paint>("-fx-text-fill",
                StyleConverter.getPaintConverter(), Color.BLACK) {

            override fun isSettable(n: TediArea): Boolean {
                val skin = n.skin as TediAreaSkin
                return !skin.textFill.isBound
            }

            override fun getStyleableProperty(n: TediArea): StyleableProperty<Paint> {
                val skin = n.skin as TediAreaSkin
                @Suppress("UNCHECKED_CAST")
                return skin.textFill as StyleableProperty<Paint>
            }
        }

        val HIGHLIGHT_FILL = object : CssMetaData<TediArea, Paint>("-fx-highlight-fill",
                StyleConverter.getPaintConverter(), Color.DODGERBLUE) {

            override fun isSettable(n: TediArea): Boolean {
                val skin = n.skin as TediAreaSkin
                return !skin.highlightFill.isBound
            }

            override fun getStyleableProperty(n: TediArea): StyleableProperty<Paint> {
                val skin = n.skin as TediAreaSkin
                @Suppress("UNCHECKED_CAST")
                return skin.highlightFill as StyleableProperty<Paint>
            }
        }

        val HIGHLIGHT_TEXT_FILL = object : CssMetaData<TediArea, Paint>("-fx-highlight-text-fill",
                StyleConverter.getPaintConverter(), Color.WHITE) {

            override fun isSettable(n: TediArea): Boolean {
                val skin = n.skin as TediAreaSkin
                return !skin.highlightTextFill.isBound
            }

            override fun getStyleableProperty(n: TediArea): StyleableProperty<Paint> {
                val skin = n.skin as TediAreaSkin
                @Suppress("UNCHECKED_CAST")
                return skin.highlightTextFill as StyleableProperty<Paint>
            }
        }

        val DISPLAY_CARET = object : CssMetaData<TediArea, Boolean>("-fx-display-caret",
                StyleConverter.getBooleanConverter(), java.lang.Boolean.TRUE) {

            override fun isSettable(n: TediArea): Boolean {
                val skin = n.skin as TediAreaSkin
                return !skin.displayCaret.isBound
            }

            override fun getStyleableProperty(n: TediArea): StyleableProperty<Boolean> {
                val skin = n.skin as TediAreaSkin
                @Suppress("UNCHECKED_CAST")
                return skin.displayCaret as StyleableProperty<Boolean>
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

        private val STYLEABLES: List<CssMetaData<out Styleable, *>>

        internal val layout = Toolkit.getToolkit().textLayoutFactory.createLayout()

        init {
            val styleables = ArrayList(TextInputControl.getClassCssMetaData())
            styleables.add(StyleableProperties.TEXT_FILL)
            styleables.add(StyleableProperties.HIGHLIGHT_FILL)
            styleables.add(StyleableProperties.HIGHLIGHT_TEXT_FILL)
            styleables.add(StyleableProperties.DISPLAY_CARET)

            STYLEABLES = Collections.unmodifiableList(styleables)
        }

        fun getClassCssMetaData(): List<CssMetaData<out Styleable, *>> = STYLEABLES

        internal fun getAscent(font: Font, boundsType: TextBoundsType): Double {
            layout.setContent("", font.impl_getNativeFont())
            layout.setWrapWidth(0f)
            layout.setLineSpacing(0f)
            if (boundsType == TextBoundsType.LOGICAL_VERTICAL_CENTER) {
                layout.setBoundsType(TextLayout.BOUNDS_CENTER)
            } else {
                layout.setBoundsType(0)
            }
            return (-layout.bounds.minY).toDouble()
        }

    }
}
