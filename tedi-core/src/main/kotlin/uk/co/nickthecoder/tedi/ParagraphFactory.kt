package uk.co.nickthecoder.tedi

import javafx.geometry.Insets
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Region
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.Text
import uk.co.nickthecoder.tedi.util.UpdatableNode
import uk.co.nickthecoder.tedi.util.VirtualFactory
import uk.co.nickthecoder.tedi.util.clamp
import uk.co.nickthecoder.tedi.util.hitTestChar

// TODO Reuse nodes (I got scrambled text when reusing)
class ParagraphFactory(val tediAreaSkin: TediAreaSkin) : VirtualFactory {

    override fun createNode(index: Int): ParagraphNode {
        return ParagraphNode(index)
    }

    override fun itemChanged(index: Int, node: Node) {
        if (node is ParagraphNode) {
            node.update(index)
        }
    }


    //--------------------------------------------------------------------------
    // ParagraphNode
    //--------------------------------------------------------------------------

    inner class ParagraphNode(index: Int) : Region(), UpdatableNode {

        private var maxTextHeight = 0.0
        private var maxTextDescent = 0.0
        private var computedWidth = 0.0
        private var calculated = false

        init {
            update(index)
        }

        private fun createText(str: String): Text {
            return Text(str).apply {
                styleClass.add("text")
                textOrigin = VPos.BASELINE
                wrappingWidth = 0.0
                font = tediAreaSkin.skinnable.font
                fill = tediAreaSkin.textFill
            }
        }

        override fun update(newIndex: Int) {
            val paragraph = tediAreaSkin.skinnable.paragraphs[newIndex]

            // println("Factory [$newIndex] -> ${paragraph.text}")

            if (tediAreaSkin.skinnable.caretLine == newIndex) {
                background = Background(BackgroundFill(tediAreaSkin.currentLineFill, CornerRadii.EMPTY, Insets.EMPTY))
            }

            calculated = false

            children.clear()

            // Find all the boundaries between highlights.
            // Using a set, because if two highlights start at the same column, we only want that column
            // in the set once.
            val splits = mutableSetOf(0, paragraph.length)
            for (highlight in paragraph.highlights) {
                splits.add(highlight.startColumn)
                splits.add(highlight.endColumn)
            }
            var splitsList = splits.sorted()

            // Edge case : A blank line will cause there to be only 1 split, but we need to force an
            // empty Text object to be created, so add and extra split.
            if (splitsList.size == 1) {
                splitsList = listOf(0, splitsList[0])
            }

            // Now we have a sorted list of column indices where the highlights change.

            // Create a Text object between each consecutive column indices in the list.
            for (i in 0..splitsList.size - 2) {
                val from = clamp(0, splitsList[i], paragraph.charSequence.length)
                val to = clamp(0, splitsList[i + 1], paragraph.charSequence.length)

                val text = createText(paragraph.charSequence.substring(from, to))

                // We may not need a background color, so don't create a Rectangle yet.
                var rectangle: Rectangle? = null

                // Find which highlight ranges apply to this part of the paragraph,
                // and apply them to the Text object. This means that each part could be
                // styled in more than one way.
                for (phr in paragraph.highlights) {
                    val highlight = phr.cause.highlight
                    if (phr.intersects(from, to)) {
                        highlight.style(text)

                        if (highlight is FillHighlight) {
                            if (rectangle == null) {
                                rectangle = Rectangle(5.0, 5.0)
                                rectangle.fill = null
                                rectangle.isSmooth = false
                                rectangle.styleClass.add("rectangle")
                            }
                            // Special case for the selection.
                            // If it the selection extends beyond this paragraph, then make the highlight rectangle the
                            // full width of the viewport
                            if (phr.cause.end > paragraph.cachedPosition + paragraph.length && highlight === tediAreaSkin.selectionHighlight) {
                                rectangle.width = FULL_WIDTH
                            }
                            // For selections, the rectangle's height is the full line height. i.e. if there are
                            // different size fonts, then the selection's highlight rectangle will be the same.
                            // even the small font's rectangle will be the full line height.
                            if (highlight === tediAreaSkin.selectionHighlight) {
                                rectangle.height = FULL_HEIGHT
                            }

                            highlight.style(rectangle)
                        }
                    }
                }
                rectangle?.let { children.add(it) }
                children.add(text)
            }
        }

        private fun calculate() {
            maxTextHeight = 0.0
            maxTextDescent = 0.0
            computedWidth = 0.0

            // Stage 1. Move all Text objects to the correct x value. Find out the maxTextHeight
            var x = 0.0
            children.forEach { child ->
                if (child is Text) {
                    child.applyCss() // TODO Is this needed?
                    val textBounds = child.boundsInLocal
                    maxTextHeight = Math.max(maxTextHeight, -textBounds.minY)
                    maxTextDescent = Math.max(maxTextDescent, textBounds.maxY)
                    computedWidth += textBounds.width

                    child.layoutX = x
                    x += textBounds.width
                }
            }

            // Round up to integer values.
            maxTextHeight = Math.ceil(maxTextHeight)
            maxTextDescent = Math.ceil(maxTextDescent)

            // Stage 2. Move all Text to the correct y value, and place rectangles in the correct places.
            var previousRectangle: Rectangle? = null
            for (child in children) {
                if (child is Text) {
                    child.layoutY = maxTextHeight
                    val textBounds = child.boundsInLocal

                    previousRectangle?.let { rectangle ->
                        // Special processing for selection highlights. See comments in update()
                        if (rectangle.width != FULL_WIDTH) {
                            rectangle.width = textBounds.width
                        }
                        rectangle.height = if (rectangle.height == FULL_HEIGHT) maxTextHeight + maxTextDescent else textBounds.height
                        rectangle.resizeRelocate(child.layoutX, maxTextHeight + maxTextDescent - rectangle.height, rectangle.width, rectangle.height)
                        previousRectangle = null
                    }

                } else if (child is Rectangle) {
                    previousRectangle = child
                }
            }

            calculated = true
        }

        override fun computePrefHeight(width: Double): Double {
            if (!calculated) {
                calculate()
            }
            return maxTextHeight + maxTextDescent
        }

        override fun computePrefWidth(height: Double): Double {
            if (!calculated) {
                calculate()
            }
            return computedWidth
        }

        override fun layoutChildren() {
            if (!calculated) {
                calculate()
            }
        }

        /**
         * Returns the column given the x coordinate (which is relative to the tedi area, not the node)
         */
        fun getColumn(x: Double): Int {
            val normX = tediAreaSkin.virtualView.toContentX(x)

            var soFar = 0
            children.forEach { text ->
                if (text is Text) { // Ignore any Rectangles which may also be in the group.
                    if (normX <= text.layoutX) {
                        return soFar
                    }
                    val insertion = text.hitTestChar(normX, 1.0).getInsertionIndex()
                    if (insertion < text.text.length) {
                        return soFar + insertion
                    }
                    soFar += text.text.length
                }
            }
            return soFar
        }

        /**
         * Returns an X coordinate in pixels (relative to the TediArea), corresponding to a column.
         * and also the Font used t that column index.
         * This is specifically designed to help position, and size the caret within TediAreaSkin.
         */
        fun caretDetails(column: Int): Pair<Double, Font?> {
            //println("xForColumn $column")
            if (column <= 0) return Pair(tediAreaSkin.virtualView.fromContentX(0.0), children.filterIsInstance<Text>().firstOrNull()?.font)

            var columnsEaten = 0
            children.forEach { text ->
                if (text is Text) { // Ignore any Rectangles which may also be in the group
                    columnsEaten += text.text.length

                    if (column == columnsEaten) {
                        // At the end of this piece of text.
                        //println("At the end of a segment ${text.boundsInParent.maxX} -> ${virtualView.fromContentX(text.boundsInParent.maxX)}")
                        return Pair(tediAreaSkin.virtualView.fromContentX(text.boundsInParent.maxX), text.font)
                    } else if (column < columnsEaten) {
                        // In the middle of the text. Let's change the text, find the new bounds, then
                        // change it back
                        val oldText = text.text
                        try {
                            text.text = oldText.substring(0, column - columnsEaten + oldText.length)
                            //println("Middle of a segment $oldText ${text.boundsInParent.maxX} -> ${virtualView.fromContentX(text.boundsInParent.maxX)}")
                            return Pair(tediAreaSkin.virtualView.fromContentX(text.boundsInParent.maxX), text.font)
                        } finally {
                            text.text = oldText
                        }

                    } else {
                        //println("Skipping ${text.text} eaten $columnsEaten")
                    }
                }
            }
            //println("At the end of loop ${boundsInLocal.maxX} -> ${virtualView.fromContentX(boundsInLocal.maxX)}")
            return Pair(tediAreaSkin.virtualView.fromContentX(boundsInLocal.maxX), children.filterIsInstance<Text>().lastOrNull()?.font)
        }
    }

    companion object {
        private val FULL_WIDTH = 5000.0
        private val FULL_HEIGHT = -1.0
    }
}
