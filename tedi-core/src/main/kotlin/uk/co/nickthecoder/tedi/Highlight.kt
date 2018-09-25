package uk.co.nickthecoder.tedi

import javafx.scene.shape.Rectangle
import javafx.scene.text.Text

interface Highlight {
    fun style(text: Text)
}

interface FillHighlight : Highlight {
    fun style(rect: Rectangle)
}

open class StyleClassHighlight(val textClass: String) : Highlight {

    override fun style(text: Text) {
        text.styleClass.add(textClass)
    }

}

open class FillStyleClassHighlight(textClass: String, val fillClass: String)
    : StyleClassHighlight(textClass), FillHighlight {

    override fun style(rect: Rectangle) {
        rect.styleClass.add(fillClass)
    }
}


open class StyleHighlight(val style: String) : Highlight {

    override fun style(text: Text) {
        text.style += style
    }

}

open class FillStyleHighlight(textStyle: String, val fillStyle: String)
    : StyleHighlight(textStyle), FillHighlight {

    override fun style(rect: Rectangle) {
        rect.style += fillStyle
    }
}

