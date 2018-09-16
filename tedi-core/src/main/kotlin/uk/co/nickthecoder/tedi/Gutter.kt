package uk.co.nickthecoder.tedi

import javafx.beans.property.ObjectProperty
import javafx.css.*
import javafx.geometry.VPos
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import java.util.*

/**
 * A rectangle to the left of the main content, showing line numbers.
 * You can style the gutter using css :
 *
 *     .tedi-area .gutter { xxx }
 */
class Gutter(val tediAreaSkin: TediAreaSkin) : Region() {

    private val lineNumbers = Text("")

    val textFill: ObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.GRAY) {
        override fun getBean() = this
        override fun getName() = "textFill"
        override fun getCssMetaData(): CssMetaData<Gutter, Paint> = TEXT_FILL
    }

    init {
        styleClass.add("gutter")
        visibleProperty().bind(tediAreaSkin.tediArea.displayLineNumbersProperty())

        children.add(lineNumbers)

        with(lineNumbers) {
            styleClass.add("text") // Must use the same style as the main content's text.
            isManaged = false
            textOrigin = VPos.TOP
            textAlignment = TextAlignment.RIGHT
            wrappingWidth = 0.0
            fontProperty().bind(tediAreaSkin.tediArea.fontProperty())
            fillProperty().bind(textFill)
        }

        updateLineNumbers()
        tediAreaSkin.tediArea.lineCountProperty().addListener { _, _, _ ->
            updateLineNumbers()
        }
    }

    private fun updateLineNumbers() {
        val lines = tediAreaSkin.tediArea.lineCount
        val buffer = StringBuffer(lines * 3)
        for (i in 1..lines) {
            buffer.append(i.toString()).append("\n")
        }
        lineNumbers.text = buffer.toString()
    }

    override fun computePrefWidth(height: Double): Double {
        var prefWidth = snappedLeftInset() + snappedRightInset()
        prefWidth += lineNumbers.prefWidth(height)
        return prefWidth
    }

    override fun layoutChildren() {
        lineNumbers.layoutX = snappedLeftInset()
        lineNumbers.layoutY = tediAreaSkin.contentView.snappedTopInset()
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
                @Suppress("UNCHECKED_CAST")
                return gutter.textFill as StyleableProperty<Paint>
            }
        }

        val STYLEABLES: List<CssMetaData<out Styleable, *>>

        init {
            val styleables = ArrayList(Region.getClassCssMetaData())
            styleables.add(TEXT_FILL)
            STYLEABLES = Collections.unmodifiableList(styleables)
        }

        fun getClassCssMetaData() = STYLEABLES
    }

}
