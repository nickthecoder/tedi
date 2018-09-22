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
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextInputControl
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Path
import javafx.scene.text.Text
import javafx.util.Duration
import uk.co.nickthecoder.tedi.javafx.BehaviorSkinBase
import java.util.*

class TediAreaSkin(val control: TediArea)

    : BehaviorSkinBase<TediArea, TediAreaBehavior>(control, TediAreaBehavior(control)) {

    /**
     * Currently, visible text is one HUGE Text object, which is inefficient, and should be
     * broken into parts.
     */
    private val paragraphNode = Text()

    /**
     * A Region containing line numbers, to the left of the main content.
     */
    private val gutter = Gutter(this)

    /**
     * A simple BorderPane with left=[gutter], center=[contentView]
     */
    private val guttersAndContentView = BorderPane()

    /**
     * The main content responsible for displaying the [paragraphNode], the caret and the selection.
     *
     * Internal, as Gutter uses this to sync its top margin with the contentView's top margin to
     * ensure the line numbers line up with the main text.
     */
    internal val contentView = ContentView()

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

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The fill to use for the text under normal conditions
     */
    private val textFill: ObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.BLACK) {

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
    private val highlightFill: ObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.DODGERBLUE) {
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

    private val highlightTextFill: ObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.WHITE) {
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

    private val displayCaret: BooleanProperty = object : StyleableBooleanProperty(true) {
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

    private val caretAnimation = CaretAnimation(caretVisible, caretPath)

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

        // paragraphNode
        with(paragraphNode) {
            text = control.text
            textOrigin = VPos.TOP
            wrappingWidth = 0.0
            isManaged = false
            styleClass.add("text")

            fontProperty().bind(control.fontProperty())
            fillProperty().bind(textFill)
        }

        // selection
        with(selectionHighlightGroup) {
            isManaged = false
            isVisible = false
        }

        // gutter
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
            strokeWidth = 1.0
            fillProperty().bind(textFill)
            strokeProperty().bind(textFill)
        }

        // contentView
        contentView.children.addAll(selectionHighlightGroup, paragraphNode, caretPath)

        // control

        control.selectionProperty().addListener { _, _, _ ->
            contentView.requestLayout()
        }

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

        control.textProperty().addListener { _ ->
            paragraphNode.text = control.textProperty().valueSafe
            targetCaretX = -1.0
            contentView.requestLayout()
        }
    }

    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
        scrollPane.resizeRelocate(contentX, contentY, contentWidth, contentHeight)
    }


    fun getCaretPosition(x: Double, y: Double): Int {
        return paragraphNode.hitTestChar(x, y).getInsertionIndex()
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

    private fun updateHighlightTextFill() {
        for (node in selectionHighlightGroup.children) {
            val selectionHighlightPath = node as Path
            selectionHighlightPath.fill = highlightFill.get()
        }
    }

    private fun updateHighlightFill() {
        for (node in selectionHighlightGroup.children) {
            val selectionHighlightPath = node as Path
            selectionHighlightPath.fill = highlightFill.get()
        }
    }

    private val tmpText = Text()

    /**
     *
     */
    private fun changeLine(n: Int, select: Boolean) {
        val lineColumn = control.lineColumnFor(control.caretPosition)

        val caretBounds = caretPath.layoutBounds
        val requiredX = if (targetCaretX < 0) caretBounds.maxX else targetCaretX

        val requiredLine = clamp(0, lineColumn.first + n, skinnable.lineCount - 1)
        val lineText = skinnable.getLine(requiredLine).toString()
        // TODO, we can use the ACTUAL paragraph node instead of tmpText when this skin uses a list of Text.
        tmpText.text = lineText
        tmpText.font = paragraphNode.font
        tmpText.layoutX = 0.0// paragraphNode.layoutX
        val hit = tmpText.hitTestChar(requiredX, 1.0)
        val columnIndex = hit.getInsertionIndex()

        val newPosition = control.positionFor(requiredLine, 0) + columnIndex

        if (select) {
            control.selectRange(control.anchor, newPosition)
        } else {
            control.selectRange(newPosition, newPosition)
        }

        targetCaretX = requiredX
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
    private class CaretAnimation(val caretVisible: ObservableBooleanValue, val caretNode: Node) {

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
                    KeyFrame(Duration.ZERO, EventHandler<ActionEvent> { blinkOn = false }),
                    KeyFrame(Duration.seconds(.5), EventHandler<ActionEvent> { blinkOn = true }),
                    KeyFrame(Duration.seconds(1.0)))
            caretVisible.addListener { _, _, newValue ->
                if (newValue == true) {
                    animation.play()
                } else {
                    animation.stop()
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
            paragraphNode.impl_caretPosition = caretPos
            caretPath.elements.clear()
            caretPath.elements.addAll(*paragraphNode.impl_caretShape)

            caretPath.layoutX = paragraphNode.layoutX
            caretPath.layoutY = paragraphNode.layoutY
            if (oldCaretBounds != caretPath.boundsInParent) {
                scrollCaretToVisible()
            }

            // Update selection fg and bg
            val start = selection.start
            val end = selection.end

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

        init {
            val styleables = ArrayList(TextInputControl.getClassCssMetaData())
            styleables.add(StyleableProperties.TEXT_FILL)
            styleables.add(StyleableProperties.HIGHLIGHT_FILL)
            styleables.add(StyleableProperties.HIGHLIGHT_TEXT_FILL)
            styleables.add(StyleableProperties.DISPLAY_CARET)

            STYLEABLES = Collections.unmodifiableList(styleables)
        }

        fun getClassCssMetaData(): List<CssMetaData<out Styleable, *>> = STYLEABLES

    }
}
