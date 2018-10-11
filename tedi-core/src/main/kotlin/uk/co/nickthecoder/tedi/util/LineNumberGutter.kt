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
import uk.co.nickthecoder.tedi.TediArea


open class LineNumberGutter(val tediArea: TediArea) : VirtualGutter {

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
    private val textFillProperty: StyleableObjectProperty<Paint> = createStyleable("textFill", Color.BLUEVIOLET, TEXT_FILL)

    fun textFill(): StyleableObjectProperty<Paint> = textFillProperty
    var textFill: Paint
        get() = textFillProperty.get()
        set(v) = textFillProperty.set(v)


    // Current Line Fill Property
    private val currentLineFillProperty: StyleableObjectProperty<Paint> = createStyleable("currentLineFill", Color.WHEAT, CURRENT_LINE_FILL)

    fun currentLineFill(): StyleableObjectProperty<Paint> = currentLineFillProperty
    var currentLineFill: Paint?
        get() = currentLineFillProperty.get()
        set(v) = currentLineFillProperty.set(v)


    // Current Line Text Fill Property
    private val currentLineTextFillProperty: StyleableObjectProperty<Paint> = createStyleable("currentLineTextFill", Color.BLACK, CURRENT_LINE_TEXT_FILL)

    fun currentLineTextFill(): StyleableObjectProperty<Paint> = currentLineTextFillProperty
    var currentLineTextFill: Paint?
        get() = currentLineTextFillProperty.get()
        set(v) = currentLineTextFillProperty.set(v)

    //---------------------------------------------------------------------------
    // Line Number Node
    //---------------------------------------------------------------------------

    open inner class LineNumberNode(var index: Int) : HBox(), UpdatableNode {

        val lineNumber = Text((index + 1).toString()).apply {
            textOrigin = VPos.TOP
        }

        init {
            style(index)
            val padding = Region()
            HBox.setHgrow(padding, Priority.ALWAYS)
            children.addAll(padding, lineNumber)
            alignment = Pos.CENTER
        }

        fun style(index: Int) {
            lineNumber.font = font
            if (index == tediArea.caretLine) {
                lineNumber.fill = currentLineTextFill
            } else {
                lineNumber.fill = textFill
            }
        }

        override fun update(newIndex: Int) {
            style(newIndex)
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
        val CURRENT_LINE_FILL = createPaintCssMetaData<GutterRegion>("-fx-current-line-fill") { it.lineGutter().currentLineFillProperty }
        val CURRENT_LINE_TEXT_FILL = createPaintCssMetaData<GutterRegion>("-fx-current-line-text-fill") { it.lineGutter().currentLineTextFillProperty }

        private val STYLEABLES: List<CssMetaData<out Styleable, *>> = listOf<CssMetaData<out Styleable, *>>(
                FONT, TEXT_FILL, CURRENT_LINE_FILL, CURRENT_LINE_TEXT_FILL)

        fun getClassCssMetaData() = STYLEABLES
    }
}

private fun GutterRegion.lineGutter(): LineNumberGutter = gutter as LineNumberGutter
