package uk.co.nickthecoder.tedi.util

import javafx.css.CssMetaData
import javafx.css.StyleConverter
import javafx.css.Styleable
import javafx.css.StyleableObjectProperty
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.text.Font
import javafx.scene.text.Text


open class LineNumberGutter : VirtualGutter() {

    // Font
    private val fontProperty: StyleableObjectProperty<Font> = object : StyleableObjectProperty<Font>(Font.getDefault()) {
        override fun getBean() = this@LineNumberGutter
        override fun getName() = "textFill"
        override fun getCssMetaData(): CssMetaData<LineNumberGutter, Font> = FONT
    }
    fun font(): StyleableObjectProperty<Font> = fontProperty
    var font: Font
        get() = fontProperty.get()
        set(v) = fontProperty.set(v)

    // Text Fill
    private val textFillProperty: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.GRAY) {
        override fun getBean() = this@LineNumberGutter
        override fun getName() = "textFill"
        override fun getCssMetaData(): CssMetaData<LineNumberGutter, Paint> = TEXT_FILL
    }
    fun textFill(): StyleableObjectProperty<Paint> = textFillProperty
    var textFill: Paint
        get() = textFillProperty.get()
        set(v) = textFillProperty.set(v)


    // Current Line Fill Property
    private val currentLineFillProperty: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.GRAY) {
        override fun getBean() = this@LineNumberGutter
        override fun getName() = "currentLineFill"
        override fun getCssMetaData() = CURRENT_LINE_FILL
    }
    fun currentLineFill(): StyleableObjectProperty<Paint> = currentLineFillProperty
    var currentLineFill: Paint?
        get() = currentLineFillProperty.get()
        set(v) = currentLineFillProperty.set(v)


    // Current Line Text Fill Property
    private val currentLineTextFillProperty: StyleableObjectProperty<Paint> = object : StyleableObjectProperty<Paint>(Color.GRAY) {
        override fun getBean() = this@LineNumberGutter
        override fun getName() = "currentLineTextFill"
        override fun getCssMetaData() = CURRENT_LINE_TEXT_FILL
    }
    fun currentLineTextFill(): StyleableObjectProperty<Paint> = currentLineTextFillProperty
    var currentLineTextFill: Paint?
        get() = currentLineTextFillProperty.get()
        set(v) = currentLineTextFillProperty.set(v)


    override fun getCssMetaData(): List<CssMetaData<out Styleable, *>> {
        return getClassCssMetaData()
    }


    override fun createNode(index: Int): LineNumberNode {
        return LineNumberNode(index)
    }

    override fun documentChanged(index: Int, node: Node) {
        if (node is LineNumberNode) {
            node.update(index)
        }
    }

    open inner class LineNumberNode(var index: Int) : HBox(), UpdatableNode {

        init {
            styleClass.add("line-number")
        }

        val lineNumber = Text((index + 1).toString()).apply {
            styleClass.add("line-number")
            textOrigin = VPos.TOP
        }

        init {
            lineNumber.font = font
            val padding = Region()
            HBox.setHgrow(padding, Priority.ALWAYS)
            children.addAll(padding, lineNumber)
        }

        override fun update(newIndex: Int) {
            lineNumber.font = font
            if (newIndex != index) {
                index = newIndex
                lineNumber.text = (index + 1).toString()
            }
        }

    }

    companion object {

        val FONT = object : CssMetaData<LineNumberGutter, Font>(
                "-fx-font",
                StyleConverter.getFontConverter(), Font.getDefault()) {
            override fun isSettable(gutter: LineNumberGutter) = !gutter.textFillProperty.isBound
            override fun getStyleableProperty(gutter: LineNumberGutter) = gutter.fontProperty
        }

        val TEXT_FILL = object : CssMetaData<LineNumberGutter, Paint>(
                "-fx-text-fill",
                StyleConverter.getPaintConverter(), Color.GREY) {
            override fun isSettable(gutter: LineNumberGutter) = !gutter.textFillProperty.isBound
            override fun getStyleableProperty(gutter: LineNumberGutter) = gutter.textFillProperty
        }

        val CURRENT_LINE_FILL = object : CssMetaData<LineNumberGutter, Paint>(
                "-fx-current-line-fill",
                StyleConverter.getPaintConverter(), Color.WHITE) {
            override fun isSettable(n: LineNumberGutter) = !n.currentLineFillProperty.isBound
            override fun getStyleableProperty(n: LineNumberGutter) = n.currentLineFillProperty
        }

        val CURRENT_LINE_TEXT_FILL = object : CssMetaData<LineNumberGutter, Paint>(
                "-fx-current-line-text-fill",
                StyleConverter.getPaintConverter(), Color.WHITE) {
            override fun isSettable(n: LineNumberGutter) = !n.currentLineTextFillProperty.isBound
            override fun getStyleableProperty(n: LineNumberGutter) = n.currentLineTextFillProperty
        }

        private val STYLEABLES: List<CssMetaData<out Styleable, *>> = extendList(Region.getClassCssMetaData(),
                FONT, TEXT_FILL, CURRENT_LINE_FILL, CURRENT_LINE_TEXT_FILL)

        fun getClassCssMetaData(): List<CssMetaData<out Styleable, *>> = STYLEABLES

    }
}
