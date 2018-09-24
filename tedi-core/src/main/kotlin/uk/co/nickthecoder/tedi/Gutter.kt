package uk.co.nickthecoder.tedi

import javafx.css.*
import javafx.geometry.VPos
import javafx.scene.Group
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text

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

    val textFill: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.GRAY) {
        override fun getBean() = this@Gutter
        override fun getName() = "textFill"
        override fun getCssMetaData(): CssMetaData<Gutter, Paint> = TEXT_FILL
    }

    val highlightTextFill: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.GRAY) {
        override fun getBean() = this@Gutter
        override fun getName() = "highlightTextFill"
        override fun getCssMetaData(): CssMetaData<Gutter, Paint> = HIGHLIGHT_TEXT_FILL
    }

    val highlightBackground: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.WHITE) {
        override fun getBean() = this@Gutter
        override fun getName() = "highlightBackground"
        override fun getCssMetaData(): CssMetaData<Gutter, Paint> = HIGHLIGHT_BACKGROUND
    }

    init {
        styleClass.add("gutter")
        visibleProperty().bind(tediArea.displayLineNumbersProperty())

        with(rectangle) {
            isManaged = false
            fillProperty().bind(highlightBackground)
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
                fillProperty().bind(textFill)
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

    fun updateCaretPosition(oldValue: Int, newValue: Int) {
        val oldLine = tediArea.lineFor(oldValue)
        val newLine = tediArea.lineFor(newValue)

        if (oldLine != newLine) {
            if (oldLine < group.children.size - 1) {
                (group.children[oldLine] as Text).fillProperty().bind(textFill)
            }
            if (newLine < group.children.size - 1) {
                (group.children[newLine] as Text).fillProperty().bind(highlightTextFill)
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
            layoutY = tediArea.lineFor(tediArea.caretPosition) * lineHeight + snappedTopInset()
        }
        border?.insets?.let { rectangle.width = width - it.right - it.left }
    }

    override fun getCssMetaData(): List<CssMetaData<out Styleable, *>> {
        return getClassCssMetaData()
    }

    companion object {

        val TEXT_FILL = object : CssMetaData<Gutter, Paint>("-fx-text-fill",
                StyleConverter.getPaintConverter(), Color.GREY) {

            override fun isSettable(gutter: Gutter): Boolean {
                return !gutter.textFill.isBound
            }

            override fun getStyleableProperty(gutter: Gutter): StyleableProperty<Paint> {
                return gutter.textFill
            }
        }

        /**
         * The color for the line number where the caret is positioned.
         */
        val HIGHLIGHT_TEXT_FILL = object : CssMetaData<Gutter, Paint>("-fx-highlight-text-fill",
                StyleConverter.getPaintConverter(), Color.GREY) {

            override fun isSettable(gutter: Gutter): Boolean {
                return !gutter.textFill.isBound
            }

            override fun getStyleableProperty(gutter: Gutter): StyleableProperty<Paint> {
                return gutter.highlightTextFill
            }
        }

        /**
         * The background color behind the line number where the caret is positioned.
         */
        val HIGHLIGHT_BACKGROUND = object : CssMetaData<Gutter, Paint>("-fx-highlight-background",
                StyleConverter.getPaintConverter(), Color.WHITE) {

            override fun isSettable(gutter: Gutter): Boolean {
                return !gutter.textFill.isBound
            }

            override fun getStyleableProperty(gutter: Gutter): StyleableProperty<Paint> {
                return gutter.highlightBackground
            }
        }

        private val STYLEABLES = extendList(Region.getClassCssMetaData(),
                TEXT_FILL, HIGHLIGHT_TEXT_FILL, HIGHLIGHT_BACKGROUND)

        fun getClassCssMetaData() = STYLEABLES

    }
}
