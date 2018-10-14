package uk.co.nickthecoder.tedi.util

import javafx.css.CssMetaData
import javafx.css.Styleable
import javafx.css.StyleableObjectProperty
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.text.Font
import javafx.scene.text.Text


open class LineNumberGutter() : VirtualGutter {

    override fun createNode(index: Int): LineNumberNode {
        return LineNumberNode(index)
    }

    override fun documentChanged(index: Int, node: Node) {
        if (node is LineNumberNode) {
            node.update(index)
        }
    }

    //---------------------------------------------------------------------------
    // Properties
    //---------------------------------------------------------------------------

    // Font
    private val fontProperty: StyleableObjectProperty<Font> = createStyleable("font", Font.getDefault(), FONT)

    fun font(): StyleableObjectProperty<Font> = fontProperty
    var font: Font
        get() = fontProperty.get()
        set(v) = fontProperty.set(v)

    // Text Fill
    private val textFillProperty: StyleableObjectProperty<Paint> = createStyleable("textFill", Color.GRAY, TEXT_FILL)

    fun textFill(): StyleableObjectProperty<Paint> = textFillProperty
    var textFill: Paint
        get() = textFillProperty.get()
        set(v) = textFillProperty.set(v)

    //---------------------------------------------------------------------------
    // Line Number Node
    //---------------------------------------------------------------------------

    open inner class LineNumberNode(var index: Int) : HBox(), UpdatableNode {

        private val lineNumber = Text((index + 1).toString()).apply {
            textOrigin = VPos.TOP
        }

        init {
            lineNumber.fontProperty().bind(fontProperty)
            lineNumber.fillProperty().bind(textFillProperty)
            val padding = Region()
            HBox.setHgrow(padding, Priority.ALWAYS)
            children.addAll(padding, lineNumber)
            alignment = Pos.CENTER
        }


        override fun update(newIndex: Int) {
            if (newIndex != index) {
                index = newIndex
                lineNumber.text = (index + 1).toString()
            }
        }

    }

    override fun getCssMetaData() = getClassCssMetaData()

    //---------------------------------------------------------------------------
    // Companion Object
    //---------------------------------------------------------------------------

    companion object {

        val FONT = createFontCssMetaData<GutterRegion>("-fx-font") { it.lineGutter().fontProperty }
        val TEXT_FILL = createPaintCssMetaData<GutterRegion>("-fx-text-fill") { it.lineGutter().textFillProperty }

        private val STYLEABLES: List<CssMetaData<out Styleable, *>> = listOf<CssMetaData<out Styleable, *>>(
                FONT, TEXT_FILL)

        fun getClassCssMetaData() = STYLEABLES
    }
}

private fun GutterRegion.lineGutter(): LineNumberGutter = gutter as LineNumberGutter
