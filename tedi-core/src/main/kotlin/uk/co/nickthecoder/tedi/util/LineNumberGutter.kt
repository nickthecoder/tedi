package uk.co.nickthecoder.tedi.util

import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.text.Text


open class LineNumberGutter : VirtualGutter {

    override fun createNode(index: Int): LineNumberNode {
        return LineNumberNode(index)
    }

    override fun documentChanged(index: Int, node: Node) {
        if (node is LineNumberNode) {
            node.update(index)
        }
    }

    open class LineNumberNode(var index: Int) : HBox() {

        val lineNumber = Text((index + 1).toString()).apply {
            styleClass.add("line-number")
            textOrigin = VPos.TOP
        }

        init {
            val padding = Region()
            HBox.setHgrow(padding, Priority.ALWAYS)
            children.addAll(padding, lineNumber)
        }

        open fun update(newIndex: Int) {
            if (newIndex != index) {
                index = newIndex
                lineNumber.text = (index + 1).toString()
            }
        }

    }

}
