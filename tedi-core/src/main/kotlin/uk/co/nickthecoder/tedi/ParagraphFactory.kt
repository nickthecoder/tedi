package uk.co.nickthecoder.tedi

import javafx.geometry.VPos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.shape.Rectangle
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

    inner class ParagraphNode(index: Int) : Group(), UpdatableNode {

        private var maxHeight = 0.0
        private var maxDescent = 0.0
        private var computedWidth = 0.0
        private var calculated = false

        init {
            update(index)
            isAutoSizeChildren = false
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
                                rectangle.isSmooth = false
                                rectangle.styleClass.add("rectangle")
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
            maxHeight = 0.0
            maxDescent = 0.0
            computedWidth = 0.0

            var x = 0.0
            var previousRectangle: Rectangle? = null
            children.forEach { child ->
                if (child is Text) {
                    child.applyCss() // TODO Is this needed?
                    val textBounds = child.boundsInLocal
                    maxHeight = Math.max(maxHeight, -textBounds.minY)
                    maxDescent = Math.max(maxDescent, textBounds.maxY)
                    computedWidth += textBounds.width

                    child.layoutX = x
                    previousRectangle?.let { rectangle ->
                        rectangle.width = textBounds.width
                        rectangle.height = textBounds.height
                        rectangle.relocate(x, 0.0)
                        previousRectangle = null
                    }
                    x += textBounds.width
                } else if (child is Rectangle) {
                    previousRectangle = child
                }

            }

            maxHeight = Math.ceil(maxHeight)
            maxDescent = Math.ceil(maxDescent)

            for (child in children) {
                if (child is Text) {
                    child.layoutY = maxHeight
                } else if (child is Rectangle) {
                    child.resize(child.width, maxHeight + maxDescent)
                }
            }

            calculated = true
        }

        override fun prefHeight(width: Double): Double {
            if (!calculated) {
                calculate()
            }
            return maxHeight + maxDescent
        }

        override fun prefWidth(height: Double): Double {
            if (!calculated) {
                calculate()
            }
            return computedWidth
        }

        override fun layoutChildren() {
            calculate()
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
         */
        fun xForColumn(column: Int): Double {
            //println("xForColumn $column")
            if (column == 0) return tediAreaSkin.virtualView.fromContentX(0.0)

            var columnsEaten = 0
            children.forEach { text ->
                if (text is Text) { // Ignore any Rectangles which may also be in the group
                    columnsEaten += text.text.length

                    if (column == columnsEaten) {
                        // At the end of this piece of text.
                        //println("At the end of a segment ${text.boundsInParent.maxX} -> ${virtualView.fromContentX(text.boundsInParent.maxX)}")
                        return tediAreaSkin.virtualView.fromContentX(text.boundsInParent.maxX)
                    } else if (column < columnsEaten) {
                        // In the middle of the text. Let's change the text, find the new bounds, then
                        // change it back
                        val oldText = text.text
                        try {
                            text.text = oldText.substring(0, column - columnsEaten + oldText.length)
                            //println("Middle of a segment $oldText ${text.boundsInParent.maxX} -> ${virtualView.fromContentX(text.boundsInParent.maxX)}")
                            return tediAreaSkin.virtualView.fromContentX(text.boundsInParent.maxX)
                        } finally {
                            text.text = oldText
                        }

                    } else {
                        //println("Skipping ${text.text} eaten $columnsEaten")
                    }
                }
            }
            //println("At the end of loop ${boundsInLocal.maxX} -> ${virtualView.fromContentX(boundsInLocal.maxX)}")
            return tediAreaSkin.virtualView.fromContentX(boundsInLocal.maxX)
        }
    }

}
