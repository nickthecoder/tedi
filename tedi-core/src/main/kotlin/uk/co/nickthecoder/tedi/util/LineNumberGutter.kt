package uk.co.nickthecoder.tedi.util

import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.text.Font
import javafx.scene.text.Text


open class LineNumberGutter(override var font: Font? = null)
    : ReusableVirtualFactory<LineNumberGutter.LineNumberNode>(), VirtualGutter {

    override fun createNewNode(index: Int): LineNumberNode {
        return LineNumberNode(index)
    }

    override fun documentChanged(index: Int, node: Node) {
        if (node is LineNumberNode) {
            node.update(index)
        }
    }

    open inner class LineNumberNode(var index: Int) : HBox(), UpdatableNode {

        val lineNumber = Text((index + 1).toString()).apply {
            styleClass.add("line-number")
            textOrigin = VPos.TOP
        }

        init {
            font?.let { lineNumber.font = it }
            val padding = Region()
            HBox.setHgrow(padding, Priority.ALWAYS)
            children.addAll(padding, lineNumber)
        }

        override fun update(newIndex: Int) {
            font?.let { lineNumber.font = it }
            if (newIndex != index) {
                index = newIndex
                lineNumber.text = (index + 1).toString()
            }
        }

    }

}
