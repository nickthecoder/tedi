package uk.co.nickthecoder.tedi

import javafx.scene.shape.Rectangle
import javafx.scene.text.Text

/**
 * Highlights a piece of Text.
 *
 * Used as part of a [HighlightRange], which can be added to [TediArea.highlightRanges].
 *
 * For information about styling Text objects see :
 * [Oracle's CSS Reference](https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#text)
 *
 */
interface Highlight {
    fun style(text: Text)
}

/**
 * Highlights a piece of Text and also a Rectangle for a background color.
 *
 * For information about styling Text objects see :
 * [Oracle's CSS Reference](https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#text),
 * and also the section on
 * [rectangles](https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#rectangle).
 *
 * Note, if TediArea ever supports line wrapping, then it is likely that
 * background colors won't be supported while wrapping is turned on.
 */
interface FillHighlight : Highlight {
    fun style(rect: Rectangle)
}

/**
 * Applies a CSS style class to the highlighted Text.
 *
 * For information about styling Text objects see :
 * [Oracle's CSS Reference](https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#text)
 *
 * Note. Use -fx-fill to select a color, NOT -fx-text-fill.
 *
 * If style == "myStyle", then include a style sheet with :
 *
 *     .myStyle {
 *         -fx-fill: red; /* chose your color! */
 *     }
 */
open class StyleClassHighlight(val textClass: String) : Highlight {

    override fun style(text: Text) {
        text.styleClass.add(textClass)
    }

    override fun toString() = "textClass ='$textClass'"
}

/**
 * Applies a CSS style class to the highlight's Text object as well as a Rectangle
 * object (for a background color).
 *
 * If textClass == "myText", and fillClass == "myBackground", then include a style sheet with :
 *
 *     .myText {
 *         -fx-fill: red;
 *     }
 *     .myBackground {
 *         -fx-fill: black;
 *     }
 *
 * If you want to use the same class names for both, then use something like this :
 *
 *     .text.myStyle {
 *         -fx-fill: red;
 *     }
 *
 *     .rectangle.myStyle {
 *         -fx-fill: black;
 *     }
 *
 * And then use "myStyle" for both [textClass] and [fillClass] (using the 2nd constructor).
 *
 * For information about styling Text objects see :
 * [Oracle's CSS Reference](https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#text),
 * and also the section on
 * [rectangles](https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#rectangle).

 */
open class FillStyleClassHighlight(textClass: String, val fillClass: String)
    : StyleClassHighlight(textClass), FillHighlight {

    /**
     * Apply the same styleClass to both the Text and the background Rectangle.
     */
    constructor(styleClass: String) : this(styleClass, styleClass)

    override fun style(rect: Rectangle) {
        rect.styleClass.add(fillClass)
    }

    override fun toString() = "textClass='$textClass' fillClass='$fillClass'"
}

/**
 * Applies a style to the highlighted Text.
 * For example, red text :
 *
 *     StyleClassHighlight( "-fx-fill: red;" )
 */
open class StyleHighlight(val textStyle: String) : Highlight {

    override fun style(text: Text) {
        text.getStyle()
        text.style += textStyle
    }

    override fun toString() = "textStyle='$textStyle'"
}

/**
 * Applies a style to the highlighted Text and a Rectangle for the background.
 * For example red text on a black background :
 *
 *     StyleClassHighlight( "-fx-fill: red;", "-fx-fill: black;" )
 */
open class FillStyleHighlight(textStyle: String, val fillStyle: String)
    : StyleHighlight(textStyle), FillHighlight {

    override fun style(rect: Rectangle) {
        rect.style += fillStyle
    }

    override fun toString() = "textStyle='$textStyle' fillStyle='$fillStyle'"
}

