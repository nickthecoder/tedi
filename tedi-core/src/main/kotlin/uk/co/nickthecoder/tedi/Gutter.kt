package uk.co.nickthecoder.tedi

import javafx.css.*
import javafx.geometry.VPos
import javafx.scene.Group
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import uk.co.nickthecoder.tedi.util.extendList

/**
 * A rectangle to the left of the main content, showing line numbers.
 * You can style the gutter using css :
 *
 *     .tedi-area .gutter { xxx }
 */
class Gutter(val tediArea: TediArea) : Region() {

    private val group = Group()

    private val rectangle = Rectangle()

    private var maxNumberWidth = 0.0

    private val textFillProperty: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.GRAY) {
        override fun getBean() = this@Gutter
        override fun getName() = "textFill"
        override fun getCssMetaData(): CssMetaData<Gutter, Paint> = TEXT_FILL
    }
    var textFill: Paint
        get() = textFillProperty.get()
        set(v) = textFillProperty.set(v)

    private val currentLineTextFillProperty: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.GRAY) {
        override fun getBean() = this@Gutter
        override fun getName() = "currentLineTextFill"
        override fun getCssMetaData(): CssMetaData<Gutter, Paint> = CURRENT_LINE_TEXT_FILL
    }
    var currentLineTextFill: Paint
        get() = currentLineTextFillProperty.get()
        set(v) = currentLineTextFillProperty.set(v)

    internal val currentLineFillProperty: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(null) {
        override fun getBean() = this@Gutter
        override fun getName() = "currentLineFill"
        override fun getCssMetaData(): CssMetaData<Gutter, Paint> = CURRENT_LINE_FILL
    }
    var currentLineFill: Paint
        get() = currentLineFillProperty.get()
        set(v) = currentLineFillProperty.set(v)

    init {
        styleClass.add("gutter")
        visibleProperty().bind(tediArea.displayLineNumbersProperty())

        with(rectangle) {
            isManaged = false
            fillProperty().bind(currentLineFillProperty)
        }

        children.addAll(rectangle, group)

        tediArea.lineCountProperty().addListener { _, _, _ ->
            updateLines()
        }

        tediArea.caretPositionProperty().addListener { _, oldValue, newValue ->
            updateCaretPosition(oldValue.toInt(), newValue.toInt())
        }

    }

    fun updateLines() {

        val required = tediArea.lineCount

        for (i in group.children.size..required - 1) {
            val text = Text((i + 1).toString())
            with(text) {
                styleClass.add("text") // Must use the same style as the main content's text.
                textOrigin = VPos.TOP
                wrappingWidth = 0.0
                isManaged = false
                fontProperty().bind(tediArea.fontProperty())
                fillProperty().bind(textFillProperty)
            }
            if (text.layoutBounds.width > maxNumberWidth) {
                maxNumberWidth = text.layoutBounds.width
                requestLayout()
            }
            group.children.add(text)
        }


        while (required < group.children.size) {
            group.children.removeAt(group.children.size - 1)
        }
    }

    private fun updateCaretPosition(oldValue: Int, newValue: Int) {
        val oldLine = tediArea.lineForPosition(oldValue)
        val newLine = tediArea.lineForPosition(newValue)

        if (oldLine != newLine) {
            if (oldLine < group.children.size - 1) {
                (group.children[oldLine] as Text).fillProperty().bind(textFillProperty)
            }
            if (newLine < group.children.size - 1) {
                (group.children[newLine] as Text).fillProperty().bind(currentLineTextFillProperty)
            }
        }
        rectangle.layoutY = snappedTopInset() + newLine * (tediArea.skin as TediAreaSkin).lineHeight()
    }

    override fun computePrefWidth(height: Double): Double {
        var prefWidth = snappedLeftInset() + snappedRightInset()
        prefWidth += maxNumberWidth
        return prefWidth
    }

    override fun layoutChildren() {

        group.layoutX = snappedLeftInset()
        group.layoutY = snappedTopInset()

        val lineHeight = (tediArea.skin as TediAreaSkin).lineHeight()
        var y = 0.0

        for (t in group.children) {
            t.layoutX = maxNumberWidth - t.boundsInLocal.width
            t.layoutY = y
            y += lineHeight
        }

        with(rectangle) {
            height = lineHeight
            layoutY = tediArea.lineForPosition(tediArea.caretPosition) * lineHeight + snappedTopInset()
        }
        border?.insets?.let { rectangle.width = width - it.right - it.left }
    }

    override fun getCssMetaData(): List<CssMetaData<out Styleable, *>> {
        return getClassCssMetaData()
    }

    companion object {

        private val TEXT_FILL = object : CssMetaData<Gutter, Paint>("-fx-text-fill",
                StyleConverter.getPaintConverter(), Color.GREY) {
            override fun isSettable(gutter: Gutter) = !gutter.textFillProperty.isBound
            override fun getStyleableProperty(gutter: Gutter) = gutter.textFillProperty
        }

        /**
         * The color for the line number where the caret is positioned.
         */
        private val CURRENT_LINE_TEXT_FILL = object : CssMetaData<Gutter, Paint>("-fx-current-line-text-fill",
                StyleConverter.getPaintConverter(), Color.GREY) {
            override fun isSettable(gutter: Gutter) = !gutter.textFillProperty.isBound
            override fun getStyleableProperty(gutter: Gutter) = gutter.currentLineTextFillProperty
        }

        /**
         * The background color behind the line number where the caret is positioned.
         */
        private val CURRENT_LINE_FILL = object : CssMetaData<Gutter, Paint>("-fx-current-line-fill",
                StyleConverter.getPaintConverter(), null) {
            override fun isSettable(gutter: Gutter) = !gutter.textFillProperty.isBound
            override fun getStyleableProperty(gutter: Gutter): StyleableProperty<Paint> = gutter.currentLineFillProperty
        }

        private val STYLEABLES = extendList(Region.getClassCssMetaData(),
                TEXT_FILL, CURRENT_LINE_TEXT_FILL, CURRENT_LINE_FILL)

        fun getClassCssMetaData() = STYLEABLES

    }
}
