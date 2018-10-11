package uk.co.nickthecoder.tedi.util

import javafx.css.CssMetaData
import javafx.css.Styleable
import javafx.scene.layout.Region


class GutterRegion(val gutter: VirtualGutter?) : Region() {

    // Make it public.
    public override fun getChildren() = super.getChildren()

    override fun layoutChildren() {
        return
    }

    private var styleableProperties: MutableList<CssMetaData<out Styleable, *>>? = null

    init {
        styleClass.add("gutter")
    }

    override fun getCssMetaData(): MutableList<CssMetaData<out Styleable, *>> {
        if (styleableProperties == null) {
            styleableProperties = mutableListOf()
            styleableProperties!!.addAll(super.getCssMetaData())
            if (gutter != null) styleableProperties!!.addAll(gutter.getCssMetaData())
            println("Gutter styles ${styleableProperties!!.map { it.property }}")
        }
        return styleableProperties!!
    }
}
