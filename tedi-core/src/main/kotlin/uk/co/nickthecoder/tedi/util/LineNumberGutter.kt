/*
Tedi
Copyright (C) 2018 Nick Robinson

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2 only, as
published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
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

open class LineNumberGutter : VirtualGutter {

    override fun createNode(index: Int): LineNumber {
        return LineNumber(index)
    }

    override fun documentChanged(index: Int, node: Node) {
        if (node is LineNumber) {
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
    private val textFillProperty: StyleableObjectProperty<Paint?> = createStyleable("textFill", Color.GRAY, TEXT_FILL)

    fun textFill(): StyleableObjectProperty<Paint?> = textFillProperty
    var textFill: Paint?
        get() = textFillProperty.get()
        set(v) = textFillProperty.set(v)

    //---------------------------------------------------------------------------
    // Line Number
    //---------------------------------------------------------------------------

    open inner class LineNumber(var index: Int) : HBox(), UpdatableNode {

        private val lineNumber = Text((index + 1).toString()).apply {
            textOrigin = VPos.TOP
        }

        init {
            styleClass.add("line-number")
            lineNumber.fontProperty().bind(fontProperty)
            lineNumber.fillProperty().bind(textFillProperty)
            val padding = Region()
            HBox.setHgrow(padding, Priority.ALWAYS)
            children.addAll(padding, lineNumber)
            alignment = Pos.CENTER
        }


        override fun update(newIndex: Int) {
            background = null
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
