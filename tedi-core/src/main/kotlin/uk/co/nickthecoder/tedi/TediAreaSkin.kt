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

import com.sun.javafx.scene.control.skin.Utils
import com.sun.javafx.scene.text.HitInfo
import com.sun.javafx.scene.text.TextLayout
import com.sun.javafx.tk.FontMetrics
import com.sun.javafx.tk.Toolkit
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.DoubleBinding
import javafx.beans.binding.IntegerBinding
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableIntegerValue
import javafx.beans.value.ObservableObjectValue
import javafx.collections.ObservableList
import javafx.css.*
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.*
import javafx.scene.AccessibleAction
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

open class TediAreaSkin(val tediArea: TediArea)

    : BehaviorSkinBase<TediArea, TediAreaBehavior>(tediArea, TediAreaBehavior(tediArea)) {

    private var computedMinWidth = java.lang.Double.NEGATIVE_INFINITY
    private var computedMinHeight = java.lang.Double.NEGATIVE_INFINITY
    private var computedPrefWidth = java.lang.Double.NEGATIVE_INFINITY
    private var computedPrefHeight = java.lang.Double.NEGATIVE_INFINITY
    private var widthForComputedPrefHeight = java.lang.Double.NEGATIVE_INFINITY
    private var characterWidth: Double = 0.0
    private var lineHeight: Double = 0.0


    private val paragraphNodes = Group()
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

    /** A shared helper object, used only by downLines().  */
    private val tmpCaretPath = Path()

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

    protected val fontMetrics: ObservableObjectValue<FontMetrics> = object : ObjectBinding<FontMetrics>() {
        init {
            bind(tediArea.fontProperty());
        }

        override fun computeValue(): FontMetrics {
            invalidateMetrics()
            return Toolkit.getToolkit().fontLoader.getFontMetrics(tediArea.font)
        }
    }


    private var caretPosition: ObservableIntegerValue = object : IntegerBinding() {
        init {
            bind(tediArea.caretPositionProperty())
        }

        override fun computeValue(): Int {
            return tediArea.caretPosition
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
            bind(tediArea.focusedProperty(), tediArea.anchorProperty(), tediArea.caretPositionProperty(),
                    tediArea.disabledProperty(), tediArea.editableProperty(), displayCaret, blink)
        }

        override fun computeValue(): Boolean {
            return !blink.get() && displayCaret.get() && tediArea.isFocused() &&
                    (tediArea.caretPosition == tediArea.anchor) &&
                    !tediArea.isDisabled() &&
                    tediArea.isEditable
        }
    }

    /***************************************************************************
     *                                                                         *
     * init                                                                    *
     *                                                                         *
     **************************************************************************/

    init {

        // Add initial text content.
        addParagraphNode(0, tediArea.text)

        if (tediArea.getOnInputMethodTextChanged() == null) {
            tediArea.setOnInputMethodTextChanged({ event -> handleInputMethodEvent(event) })
        }

        caretPosition.addListener { _, oldValue, newValue ->
            targetCaretX = -1.0
            if (newValue.toInt() > oldValue.toInt()) {
                setForwardBias(true)
            }
        }

        forwardBias.addListener({ _ ->
            if (tediArea.width > 0) {
                updateTextNodeCaretPos(tediArea.caretPosition)
            }
        })

        // Initialize content
        scrollPane.isFitToWidth = false
        scrollPane.content = guttersAndContentView
        children.add(scrollPane)

        // Add selection
        selectionHighlightGroup.isManaged = false
        selectionHighlightGroup.isVisible = false
        contentView.children.add(selectionHighlightGroup)

        // Add content view
        paragraphNodes.isManaged = false
        contentView.children.addAll(paragraphNodes)


        // gutter
        guttersAndContentView.left = if (gutter.isVisible) gutter else null
        guttersAndContentView.center = contentView

        tediArea.displayLineNumbersProperty().addListener { _, _, _ ->
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

        tediArea.selectionProperty().addListener { _, _, _ ->
            // Why do we need two calls here? (from original)
            tediArea.requestLayout()
            contentView.requestLayout()
        }

        tediArea.prefColumnCountProperty().addListener { _, _, _ ->
            invalidateMetrics()
            updatePrefViewportWidth()
        }

        tediArea.prefRowCountProperty().addListener { _, _, _ ->
            invalidateMetrics()
            updatePrefViewportHeight()
        }

        updateFontMetrics()
        fontMetrics.addListener({ _ -> updateFontMetrics() })

        contentView.paddingProperty().addListener { _ ->
            updatePrefViewportWidth()
            updatePrefViewportHeight()
        }
        // TODO, I probably need more such calls, for when the gutter's padding changes???

        scrollPane.viewportBoundsProperty().addListener { _ ->
            if (scrollPane.viewportBounds != null) {
                // ScrollPane creates a new Bounds instance for each
                // layout pass, so we need to check if the width/height
                // have really changed to avoid infinite layout requests.
                val newViewportBounds = scrollPane.viewportBounds
                if (oldViewportBounds == null ||
                        oldViewportBounds?.width != newViewportBounds.width ||
                        oldViewportBounds?.height != newViewportBounds.height) {

                    invalidateMetrics()
                    oldViewportBounds = newViewportBounds
                    contentView.requestLayout()
                }
            }
        }

        tediArea.scrollTopProperty().addListener { _, _, newValue ->
            val vValue = if (newValue.toDouble() < getScrollTopMax())
                newValue.toDouble() / getScrollTopMax()
            else
                1.0
            scrollPane.vvalue = vValue
        }

        tediArea.scrollLeftProperty().addListener { _, _, newValue ->
            val hValue = if (newValue.toDouble() < getScrollLeftMax())
                newValue.toDouble() / getScrollLeftMax()
            else
                1.0
            scrollPane.hvalue = hValue
        }

        tediArea.textProperty().addListener { _ ->
            invalidateMetrics()
            (paragraphNodes.children[0] as Text).text = tediArea.textProperty().valueSafe
            contentView.requestLayout()
        }

        updateHighlightFill()
        updatePrefViewportWidth()
        updatePrefViewportHeight()
        if (tediArea.isFocused) setCaretAnimating(true)

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

    protected fun invalidateMetrics() {
        computedMinWidth = java.lang.Double.NEGATIVE_INFINITY
        computedMinHeight = java.lang.Double.NEGATIVE_INFINITY
        computedPrefWidth = java.lang.Double.NEGATIVE_INFINITY
        computedPrefHeight = java.lang.Double.NEGATIVE_INFINITY
    }

    private fun updateGutters() {
        if (tediArea.displayLineNumbers) {
            guttersAndContentView.left = gutter
        } else {
            guttersAndContentView.left = null
        }
    }

    override fun layoutChildren(contentX: Double, contentY: Double, contentWidth: Double, contentHeight: Double) {
        scrollPane.resizeRelocate(contentX, contentY, contentWidth, contentHeight)
    }

    private fun addParagraphNode(i: Int, string: String) {
        val textArea = skinnable
        val paragraphNode = Text(string)

        with(paragraphNode) {
            textOrigin = VPos.TOP
            wrappingWidth = 0.0
            isManaged = false
            styleClass.add("text")
            boundsTypeProperty().addListener { _, _, _ ->
                invalidateMetrics()
                updateFontMetrics()
            }

            fontProperty().bind(textArea.fontProperty())
            fillProperty().bind(textFill)
            impl_selectionFillProperty().bind(highlightTextFill)
        }
        paragraphNodes.children.add(i, paragraphNode)

    }

    public override fun computeBaselineOffset(topInset: Double, rightInset: Double, bottomInset: Double, leftInset: Double): Double {
        val firstParagraph = paragraphNodes.children[0] as Text
        return getAscent(skinnable.font, firstParagraph.boundsType) + contentView.snappedTopInset() + tediArea.snappedTopInset()
    }

    fun getCharacter(index: Int): Char {
        val n = paragraphNodes.children.size

        var paragraphIndex = 0
        var offset = index

        var paragraph: String? = null
        while (paragraphIndex < n) {
            val paragraphNode = paragraphNodes.children[paragraphIndex] as Text
            paragraph = paragraphNode.text
            val count = paragraph!!.length + 1

            if (offset < count) {
                break
            }

            offset -= count
            paragraphIndex++
        }

        return if (offset == paragraph!!.length) '\n' else paragraph[offset]
    }

    fun getInsertionPoint(x: Double, y: Double): Int {
        val textArea = skinnable

        val n = paragraphNodes.children.size
        var index = -1

        if (n > 0) {
            if (y < contentView.snappedTopInset()) {
                // Select the character at x in the first row
                val paragraphNode = paragraphNodes.children[0] as Text
                index = getNextInsertionPoint(paragraphNode, x, -1, VerticalDirection.DOWN)
            } else if (y > contentView.snappedTopInset() + contentView.height) {
                // Select the character at x in the last row
                val lastParagraphIndex = n - 1
                val lastParagraphView = paragraphNodes.children[lastParagraphIndex] as Text

                index = getNextInsertionPoint(lastParagraphView, x, -1, VerticalDirection.UP) + (textArea.length - lastParagraphView.text.length)
            } else {
                // Select the character at x in the row at y
                var paragraphOffset = 0
                for (i in 0..n - 1) {
                    val paragraphNode = paragraphNodes.children[i] as Text

                    val bounds = paragraphNode.boundsInLocal
                    val paragraphViewY = paragraphNode.layoutY + bounds.minY
                    if (y >= paragraphViewY && y < paragraphViewY + paragraphNode.boundsInLocal.height) {
                        index = getInsertionPoint(paragraphNode,
                                x - paragraphNode.layoutX,
                                y - paragraphNode.layoutY) + paragraphOffset
                        break
                    }

                    paragraphOffset += paragraphNode.text.length + 1
                }
            }
        }

        return index
    }

    fun positionCaret(hit: HitInfo, select: Boolean, extendSelection: Boolean) {

        var pos = Utils.getHitInsertionIndex(hit, skinnable.text)
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

    private fun getInsertionPoint(paragraphNode: Text, x: Double, y: Double): Int {
        val hitInfo = paragraphNode.impl_hitTestChar(Point2D(x, y))
        return Utils.getHitInsertionIndex(hitInfo, paragraphNode.text)
    }

    private fun getNextInsertionPoint(paragraphNode: Text, x: Double, from: Int,
                                      scrollDirection: VerticalDirection): Int {
        // TODO
        return 0
    }

    fun getCharacterBounds(index: Int): Rectangle2D {
        val textArea = skinnable

        var paragraphIndex = paragraphNodes.children.size
        var paragraphOffset = textArea.length + 1

        var paragraphNode: Text?
        do {
            paragraphNode = paragraphNodes.children[--paragraphIndex] as Text
            paragraphOffset -= paragraphNode.text.length + 1
        } while (index < paragraphOffset)

        var characterIndex = index - paragraphOffset
        var terminator = false

        if (characterIndex == paragraphNode!!.text.length) {
            characterIndex--
            terminator = true
        }

        characterBoundingPath.elements.clear()
        characterBoundingPath.elements.addAll(*paragraphNode.impl_getRangeShape(characterIndex, characterIndex + 1))
        characterBoundingPath.layoutX = paragraphNode.layoutX
        characterBoundingPath.layoutY = paragraphNode.layoutY

        val bounds = characterBoundingPath.boundsInLocal

        var x = bounds.minX + paragraphNode.layoutX - textArea.scrollLeft
        val y = bounds.minY + paragraphNode.layoutY - textArea.scrollTop

        // Sometimes the bounds is empty, in which case we must ignore the width/height
        var width = if (bounds.isEmpty) 0.0 else bounds.width
        val height = if (bounds.isEmpty) 0.0 else bounds.height

        if (terminator) {
            x += width
            width = 0.0
        }

        return Rectangle2D(x, y, width, height)
    }

    fun scrollCharacterToVisible(index: Int) {
        // TODO We queue a callback because when characters are added or
        // removed the bounds are not immediately updated; is this really
        // necessary?

        Platform.runLater {
            if (skinnable.length != 0) {
                val characterBounds = getCharacterBounds(index)
                scrollBoundsToVisible(characterBounds)
            }
        }
    }

    private fun scrollCaretToVisible() {
        val textArea = skinnable
        val bounds = caretPath.getLayoutBounds()
        val x = bounds.getMinX() - textArea.scrollLeft
        val y = bounds.getMinY() - textArea.scrollTop
        val w = bounds.getWidth()
        val h = bounds.getHeight()

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

    private fun updatePrefViewportWidth() {
        val columnCount = skinnable.prefColumnCount
        scrollPane.prefViewportWidth = columnCount * characterWidth + contentView.snappedLeftInset() + contentView.snappedRightInset()
        scrollPane.minViewportWidth = characterWidth + contentView.snappedLeftInset() + contentView.snappedRightInset()
    }

    private fun updatePrefViewportHeight() {
        val rowCount = skinnable.prefRowCount
        scrollPane.prefViewportHeight = rowCount * lineHeight + contentView.snappedTopInset() + contentView.snappedBottomInset()
        scrollPane.minViewportHeight = lineHeight + contentView.snappedTopInset() + contentView.snappedBottomInset()
    }

    private fun updateFontMetrics() {
        val firstParagraph = paragraphNodes.children[0] as Text
        lineHeight = getLineHeight(skinnable.font, firstParagraph.boundsType)
        characterWidth = fontMetrics.get().computeStringWidth("W").toDouble()
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

    private fun getTextTranslateY(): Double {
        return contentView.snappedTopInset()
    }

    private fun translateCaretPosition(p: Point2D): Point2D {
        return p
    }

    private fun getTextNode(): Text {
        return paragraphNodes.children[0] as Text
    }

    fun getIndex(x: Double, y: Double): HitInfo {
        // adjust the event to be in the same coordinate space as the
        // text content of the textInputControl
        val textNode = getTextNode()
        val p = Point2D(x - textNode.layoutX, y - getTextTranslateY())
        val hit = textNode.impl_hitTestChar(translateCaretPosition(p))
        val pos = hit.charIndex
        if (pos > 0) {
            val oldPos = textNode.impl_caretPosition
            textNode.impl_caretPosition = pos
            val element = textNode.impl_caretShape[0]
            if (element is MoveTo && element.y > y - getTextTranslateY()) {
                hit.charIndex = pos - 1
            }
            textNode.impl_caretPosition = oldPos
        }
        return hit
    };

    protected fun downLines(nLines: Int, select: Boolean, extendSelection: Boolean) {
        val textNode = getTextNode()
        val caretBounds = caretPath.layoutBounds

        // The middle y coordinate of the the line we want to go to.
        var targetLineMidY = (caretBounds.minY + caretBounds.maxY) / 2 + nLines * lineHeight
        if (targetLineMidY < 0) {
            targetLineMidY = 0.0
        }

        // The target x for the caret. This may have been set during a
        // previous call.
        val x = if (targetCaretX >= 0) targetCaretX else caretBounds.maxX

        // Find a text position for the target x,y.
        val hit = textNode.impl_hitTestChar(translateCaretPosition(Point2D(x, targetLineMidY)))
        val pos = hit.charIndex

        // Save the old pos temporarily while testing the new one.
        val oldPos = textNode.impl_caretPosition
        val oldBias = textNode.isImpl_caretBias
        textNode.isImpl_caretBias = hit.isLeading
        textNode.impl_caretPosition = pos
        tmpCaretPath.elements.clear()
        tmpCaretPath.elements.addAll(*textNode.impl_caretShape)
        tmpCaretPath.layoutX = textNode.layoutX
        tmpCaretPath.layoutY = textNode.layoutY
        val tmpCaretBounds = tmpCaretPath.layoutBounds
        // The y for the middle of the row we found.
        val foundLineMidY = (tmpCaretBounds.minY + tmpCaretBounds.maxY) / 2
        textNode.isImpl_caretBias = oldBias
        textNode.impl_caretPosition = oldPos

        if (pos > 0) {
            if (nLines > 0 && foundLineMidY > targetLineMidY) {
                // We went too far and ended up after a newline.
                hit.charIndex = pos - 1
            }

            if (pos >= tediArea.length && getCharacter(pos - 1) == '\n') {
                // Special case for newline at end of text.
                hit.isLeading = true
            }
        }

        // Test if the found line is in the correct direction and move
        // the caret.
        if (nLines == 0 ||
                (nLines > 0 && foundLineMidY > caretBounds.maxY) ||
                (nLines < 0 && foundLineMidY < caretBounds.minY)) {

            positionCaret(hit, select, extendSelection)
            targetCaretX = x
        }

    }

    fun previousLine(select: Boolean) {
        downLines(-1, select, false)
    }

    fun nextLine(select: Boolean) {
        downLines(1, select, false)
    }

    fun previousPage(select: Boolean) {
        downLines(-(scrollPane.viewportBounds.height / lineHeight).toInt(),
                select, false)
    }

    fun nextPage(select: Boolean) {
        downLines((scrollPane.viewportBounds.height / lineHeight).toInt(),
                select, false)
    }

    fun lineStart(select: Boolean, extendSelection: Boolean) {
        targetCaretX = 0.0
        downLines(0, select, extendSelection)
        targetCaretX = -1.0
    }

    fun lineEnd(select: Boolean, extendSelection: Boolean) {
        targetCaretX = java.lang.Double.MAX_VALUE
        downLines(0, select, extendSelection)
        targetCaretX = -1.0
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
        val textNode = getTextNode()
        if (isForwardBias()) {
            textNode.impl_caretPosition = pos
        } else {
            textNode.impl_caretPosition = pos - 1
        }
        textNode.impl_caretBiasProperty().set(isForwardBias())
    }

    protected fun getUnderlineShape(start: Int, end: Int): Array<PathElement>? {
        var pStart = 0
        for (node in paragraphNodes.children) {
            val p = node as Text
            val pEnd = pStart + p.textProperty().valueSafe.length
            if (pEnd >= start) {
                return p.impl_getUnderlineShape(start - pStart, end - pStart)
            }
            pStart = pEnd + 1
        }
        return null
    }

    protected fun getRangeShape(start: Int, end: Int): Array<out PathElement>? {
        var pStart = 0
        for (node in paragraphNodes.children) {
            val p = node as Text
            val pEnd = pStart + p.textProperty().valueSafe.length
            if (pEnd >= start) {
                return p.impl_getRangeShape(start - pStart, end - pStart)
            }
            pStart = pEnd + 1
        }
        return null
    }

    protected fun addHighlight(nodes: List<Node>, start: Int) {
        var pStart = 0
        var paragraphNode: Text? = null
        for (node in paragraphNodes.children) {
            val p = node as Text
            val pEnd = pStart + p.textProperty().valueSafe.length
            if (pEnd >= start) {
                paragraphNode = p
                break
            }
            pStart = pEnd + 1
        }

        if (paragraphNode != null) {
            for (node in nodes) {
                node.layoutX = paragraphNode.layoutX
                node.layoutY = paragraphNode.layoutY
            }
        }
        contentView.getChildren().addAll(nodes)
    }

    protected fun removeHighlight(nodes: List<Node>) {
        contentView.getChildren().removeAll(nodes)
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
                val text = getTextNode()
                return text.queryAccessibleAttribute(attribute, *parameters)
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
    private val imattrs = java.util.ArrayList<Shape>()

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

    protected fun isRTL(): Boolean {
        return skinnable.effectiveNodeOrientation == NodeOrientation.RIGHT_TO_LEFT
    };

    fun setCaretAnimating(value: Boolean) {
        if (value) {
            caretBlinking.start()
        } else {
            caretBlinking.stop()
            blink.set(true)
        }
    }

    override fun executeAccessibleAction(action: AccessibleAction?, vararg parameters: Any) {
        when (action) {
            AccessibleAction.SHOW_TEXT_RANGE -> {
                val start = parameters[0] as Int
                val end = parameters[1] as Int
                scrollCharacterToVisible(end)
                scrollCharacterToVisible(start)
                scrollCharacterToVisible(end)
            }
            else -> super.executeAccessibleAction(action, *parameters)
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
            if (computedPrefWidth < 0) {
                var prefWidth = 0.0

                for (node in paragraphNodes.getChildren()) {
                    val paragraphNode = node as Text
                    prefWidth = Math.max(prefWidth,
                            computeTextWidth(paragraphNode.font, paragraphNode.text, 0.0))
                }

                prefWidth += snappedLeftInset() + snappedRightInset()

                if (gutter.isVisible) {
                    prefWidth += gutter.prefWidth(height)
                }

                val viewPortBounds = scrollPane.viewportBounds
                computedPrefWidth = Math.max(prefWidth, if (viewPortBounds != null) viewPortBounds.width else 0.0)
            }
            return computedPrefWidth
        }

        override fun computePrefHeight(width: Double): Double {
            if (width != widthForComputedPrefHeight) {
                invalidateMetrics()
                widthForComputedPrefHeight = width
            }

            if (computedPrefHeight < 0) {

                var prefHeight = 0.0

                for (node in paragraphNodes.children) {
                    val paragraphNode = node as Text
                    prefHeight += computeTextHeight(
                            paragraphNode.font,
                            paragraphNode.text,
                            0.0,
                            paragraphNode.boundsType)
                }

                prefHeight += snappedTopInset() + snappedBottomInset()

                val viewPortBounds = scrollPane.getViewportBounds()
                computedPrefHeight = Math.max(prefHeight, if (viewPortBounds != null) viewPortBounds.height else 0.0)
            }
            return computedPrefHeight
        }

        override fun computeMinWidth(height: Double): Double {
            if (computedMinWidth < 0) {
                val hInsets = snappedLeftInset() + snappedRightInset()
                computedMinWidth = Math.min(characterWidth + hInsets, computePrefWidth(height))
            }
            return computedMinWidth
        }

        override fun computeMinHeight(width: Double): Double {
            if (computedMinHeight < 0) {
                val vInsets = snappedTopInset() + snappedBottomInset()
                computedMinHeight = Math.min(lineHeight + vInsets, computePrefHeight(width))
            }
            return computedMinHeight
        }

        public override fun layoutChildren() {

            val tediArea = skinnable
            val width = width
            val height = height

            // Lay out paragraphs
            val topPadding = snappedTopInset()
            val leftPadding = snappedLeftInset()

            val x = leftPadding
            var y = topPadding

            val paragraphNodesChildren = paragraphNodes.children

            for (i in paragraphNodesChildren.indices) {
                val node = paragraphNodesChildren.get(i)
                val paragraphNode = node as Text

                val bounds = paragraphNode.boundsInLocal
                paragraphNode.layoutX = x
                paragraphNode.layoutY = y

                y += bounds.height
            }

            // Update the selection
            val selection = tediArea.selection
            val oldCaretBounds = caretPath.boundsInParent

            selectionHighlightGroup.children.clear()

            val caretPos = tediArea.caretPosition

            // Position caret
            var paragraphIndex = paragraphNodesChildren.size
            var paragraphOffset = tediArea.length + 1

            var paragraphNode: Text?
            do {
                paragraphNode = (paragraphNodesChildren.get(--paragraphIndex)) as Text
                paragraphOffset -= paragraphNode.text.length + 1
            } while (caretPos < paragraphOffset)

            updateTextNodeCaretPos(caretPos - paragraphOffset)
            caretPath.elements.clear()
            caretPath.elements.addAll(*paragraphNode!!.impl_caretShape)

            caretPath.layoutX = paragraphNode.layoutX

            // TODO: Remove this temporary workaround for RT-27533
            paragraphNode.layoutX = 2 * paragraphNode.layoutX - paragraphNode.boundsInParent.minX

            caretPath.layoutY = paragraphNode.layoutY
            if (oldCaretBounds == null || oldCaretBounds != caretPath.boundsInParent) {
                scrollCaretToVisible()
            }

            // Update selection fg and bg
            var start = selection.start
            var end = selection.end
            var i = 0
            val max = paragraphNodesChildren.size
            while (i < max) {
                val paragraphNode = paragraphNodesChildren.get(i)
                val textNode = paragraphNode as Text
                val paragraphLength = textNode.text.length + 1
                if (end > start && start < paragraphLength) {
                    textNode.impl_selectionStart = start
                    textNode.impl_selectionEnd = Math.min(end, paragraphLength)

                    val selectionHighlightPath = Path()
                    selectionHighlightPath.isManaged = false
                    selectionHighlightPath.stroke = null
                    val selectionShape = textNode.impl_selectionShape
                    if (selectionShape != null) {
                        selectionHighlightPath.elements.addAll(*selectionShape)
                    }
                    selectionHighlightGroup.getChildren().add(selectionHighlightPath)
                    selectionHighlightGroup.setVisible(true)
                    selectionHighlightPath.layoutX = textNode.layoutX
                    selectionHighlightPath.layoutY = textNode.layoutY
                    updateHighlightFill()
                } else {
                    textNode.impl_selectionStart = -1
                    textNode.impl_selectionEnd = -1
                    selectionHighlightGroup.setVisible(false)
                }
                start = Math.max(0, start - paragraphLength)
                end = Math.max(0, end - paragraphLength)
                i++
            }

            if (scrollPane.prefViewportWidth == 0.0 || scrollPane.prefViewportHeight == 0.0) {
                updatePrefViewportWidth()
                updatePrefViewportHeight()
                if (parent != null && scrollPane.prefViewportWidth > 0 || scrollPane.prefViewportHeight > 0) {
                    // Force layout of viewRect in ScrollPaneSkin
                    parent.requestLayout()
                }
            }

            // RT-36454: Fit to width/height only if smaller than viewport.
            // That is, grow to fit but don't shrink to fit.
            val viewportBounds = scrollPane.viewportBounds
            val wasFitToWidth = scrollPane.isFitToWidth()
            val wasFitToHeight = scrollPane.isFitToHeight()
            val setFitToWidth = computePrefWidth(-1.0) <= viewportBounds.width
            val setFitToHeight = computePrefHeight(width) <= viewportBounds.height

            if (wasFitToWidth != setFitToWidth || wasFitToHeight != setFitToHeight) {
                Platform.runLater {
                    scrollPane.setFitToWidth(setFitToWidth)
                    scrollPane.setFitToHeight(setFitToHeight)
                }
                parent.requestLayout()
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

        internal fun computeTextWidth(font: Font, text: String?, wrappingWidth: Double): Double {
            layout.setContent(text ?: "", font.impl_getNativeFont())
            layout.setWrapWidth(wrappingWidth.toFloat())
            return layout.getBounds().getWidth().toDouble()
        }

        internal fun computeTextHeight(font: Font, text: String, wrappingWidth: Double, boundsType: TextBoundsType): Double {
            return computeTextHeight(font, text, wrappingWidth, 0.0, boundsType)
        }

        internal fun computeTextHeight(font: Font, text: String?, wrappingWidth: Double, lineSpacing: Double, boundsType: TextBoundsType): Double {
            layout.setContent(text ?: "", font.impl_getNativeFont())
            layout.setWrapWidth(wrappingWidth.toFloat())
            layout.setLineSpacing(lineSpacing.toFloat())
            if (boundsType == TextBoundsType.LOGICAL_VERTICAL_CENTER) {
                layout.setBoundsType(TextLayout.BOUNDS_CENTER)
            } else {
                layout.setBoundsType(0)
            }
            return layout.bounds.height.toDouble()
        }

        internal fun getLineHeight(font: Font, boundsType: TextBoundsType): Double {
            layout.setContent("", font.impl_getNativeFont())
            layout.setWrapWidth(0f)
            layout.setLineSpacing(0f)
            if (boundsType == TextBoundsType.LOGICAL_VERTICAL_CENTER) {
                layout.setBoundsType(TextLayout.BOUNDS_CENTER)
            } else {
                layout.setBoundsType(0)
            }

            // RT-37092: Use the line bounds specifically, to include font leading.
            return layout.lines[0].bounds.height.toDouble()
        }

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
