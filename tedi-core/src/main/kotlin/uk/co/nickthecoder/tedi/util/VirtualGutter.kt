package uk.co.nickthecoder.tedi.util

import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.layout.Region
import javafx.scene.text.Font
import javafx.scene.text.Text

/**
 * Creates [Node]s to the left of [VirtualView], typically used to display line numbers.
 *
 * The nodes are virtual, i.e. only the visible items are created.
 *
 * While the primary use case is for line numbers, a gutter can be useful for other purposes. For example :
 *
 * - A button to mark lines as "important" (can be used in conjunction with HighlightRanges)
 * - Colour the gutter with information from git (lines added, lines modified)
 * - Add other meta-data, such as who is responsible for that part of the document.
 * - Controls for expand/contract for a folding editor (though this would probably require support from TediArea itself)
 * - Controls for manipulating the whole line (e.g. delete a line with a single click).
 * - Adding "check marks" to lines, which can then be processed from other parts of your GUI.
 *
 * I suggest extending [LineNumberGutter], if you want to add extra features to your gutters.
 */
interface VirtualGutter : VirtualFactory {

    /**
     * Called whenever ANY changes are made to VirtualView's list, i.e.,
     * insertions, deletions and updates.
     *
     * This solely designed to allow line numbers to be updated when paragraphs are added/deleted.
     *
     * @param index An index into VirtualView's list (i.e. the Paragraph's index for TediArea's gutter).
     *
     * @param node The corresponding node created earlier via [createNode]
     */
    fun documentChanged(index: Int, node: Node) {}

}

/**
 * An implementation of [VirtualGutter], whose nodes can contain many pieces of information, layed out
 * horizontally, with each part vertically centered.
 */
abstract class BoxGutter : VirtualGutter {

    override fun documentChanged(index: Int, node: Node) {
        if (node is GutterNode) {
            node.update(index)
        }
    }

    abstract class GutterNode : Region() {

        val spacing = 10.0 // TODO Make this styleable

        init {
            style = "-fx-background-color: red;"
            styleClass.add("gutter-node")
        }

        abstract fun update(newIndex: Int)

        override fun computePrefWidth(height: Double): Double {
            var total = snappedLeftInset() + snappedRightInset() - spacing
            for (child in children) {
                total += child.prefWidth(height) + spacing
            }
            return total
        }

        override fun layoutChildren() {
            val height = height

            var x = width - snappedRightInset()
            // Lay out from right to left, because the line number is on the left, and this makes it easy
            // to right align it.

            for (i in children.size - 1 downTo 0) {
                val child = children[i]
                val childWidth = child.prefWidth(height)
                val childHeight = child.prefHeight(-1.0)
                x -= childWidth
                child.resizeRelocate(x, (height - childHeight) / 2, childWidth, childHeight)
                x -= spacing
            }
        }
    }
}

open class LineNumberGutter : BoxGutter() {

    override fun createNode(index: Int): Node {
        return LineNumberNode(index)
    }

    open class LineNumberNode(var index: Int) : GutterNode() {

        val lineNumber = Text((index + 1).toString()).apply { textOrigin = VPos.TOP }

        init {
            children.addAll(lineNumber, Text("Hi").apply { font = Font.font(font.name, 6.0) })
        }

        override fun update(newIndex: Int) {
            if (newIndex != index) {
                index = newIndex
                lineNumber.text = (index + 1).toString()
            }
        }

    }

}
