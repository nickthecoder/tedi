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
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.paint.Paint


open class GutterRegion(val gutter: VirtualGutter?) : Region() {


    // Text Fill
    private val currentLineFillProperty: StyleableObjectProperty<Paint?> = createStyleable("textFill", Color.GRAY, CURRENT_LINE_FILL)

    fun currentLineFill(): StyleableObjectProperty<Paint?> = currentLineFillProperty
    var currentLineFill: Paint?
        get() = currentLineFillProperty.get()
        set(v) = currentLineFillProperty.set(v)


    // Make it public.
    public override fun getChildren() = super.getChildren()

    override fun layoutChildren() {
        return
    }

    private var styleableProperties: MutableList<CssMetaData<out Styleable, *>>? = null

    init {
        styleClass.add("gutter")
    }

    override fun getCssMetaData(): List<CssMetaData<out Styleable, *>> {
        if (gutter == null) {
            return super.getCssMetaData()
        } else {
            if (styleableProperties == null) {
                styleableProperties = mutableListOf()
                styleableProperties!!.addAll(super.getCssMetaData())
                styleableProperties!!.addAll(gutter.getCssMetaData())
            }
            return styleableProperties!!
        }
    }

    companion object {
        val CURRENT_LINE_FILL = createPaintCssMetaData<GutterRegion>("-fx-text-fill") { it.currentLineFillProperty }
    }
}
