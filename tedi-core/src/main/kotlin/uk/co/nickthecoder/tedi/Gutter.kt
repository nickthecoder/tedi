package uk.co.nickthecoder.tedi

import javafx.beans.property.ObjectProperty
import javafx.css.*
import javafx.geometry.VPos
import javafx.scene.Group
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.text.Text
import java.util.*

/**
 * A rectangle to the left of the main content, showing line numbers.
 * You can style the gutter using css :
 *
 *     .tedi-area .gutter { xxx }
 */
class Gutter(val tediAreaSkin: TediAreaSkin) : Region() {

    private val group = Group()

    private var maxNumberWidth = 0.0

    val textFill: ObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.GRAY) {
        override fun getBean() = this
        override fun getName() = "textFill"
        override fun getCssMetaData(): CssMetaData<Gutter, Paint> = TEXT_FILL
    }

    init {
        styleClass.add("gutter")
        visibleProperty().bind(tediAreaSkin.control.displayLineNumbersProperty())

        children.add(group)

        tediAreaSkin.control.lineCountProperty().addListener { _, _, _ ->
            updateLines()
        }
    }

    fun updateLines() {

        val required = tediAreaSkin.control.lineCount

        for (i in group.children.size..required - 1) {
            val text = Text((i + 1).toString())
            with(text) {
                styleClass.add("text") // Must use the same style as the main content's text.
                isManaged = false
                textOrigin = VPos.TOP
                wrappingWidth = 0.0
                layoutY = tediAreaSkin.lineHeight() * i
                fontProperty().bind(tediAreaSkin.control.fontProperty())
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

    override fun computePrefWidth(height: Double): Double {
        var prefWidth = snappedLeftInset() + snappedRightInset()
        prefWidth += maxNumberWidth
        return prefWidth
    }

    override fun layoutChildren() {
        group.layoutX = snappedLeftInset()
        group.layoutY = tediAreaSkin.contentView.snappedTopInset()
        for (t in group.children) {
            t.layoutX = maxNumberWidth - t.boundsInLocal.width
        }
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
