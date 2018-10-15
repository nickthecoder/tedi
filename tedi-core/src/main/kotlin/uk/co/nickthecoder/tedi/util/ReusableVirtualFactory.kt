package uk.co.nickthecoder.tedi.util

import javafx.scene.Node

interface UpdatableNode {
    fun update(newIndex: Int)
}

/**
 * When nodes are freed, they are added to a list.
 * The next time [createNode] is called, instead of creating a new node from scratch,
 * a freed node is reused.
 *
 * [maxSize] is the maximum number of nodes to store in the freed list.
 * If it is smaller than the number of visible nodes, then new nodes will still be created on a regular
 * basis (evey time the viewport is paged up or down).
 *
 */
open class ReusableVirtualFactory(protected val wrapped: VirtualFactory, val maxSize: Int = 100)
    : VirtualFactory {

    protected val reusableList = mutableListOf<Node>()

    override fun createNode(index: Int): Node = if (reusableList.isEmpty()) {
        wrapped.createNode(index)
    } else {
        reusableList.removeAt(reusableList.size - 1).apply {
            if (this is UpdatableNode) {
                update(index)
            }
        }
    }

    override fun free(index: Int, node: Node) {
        if (reusableList.size < maxSize) {
            reusableList.add(node)
        }
    }
}

open class ReusableVirtualGutter(protected val wrappedGutter: VirtualGutter, maxSize: Int = 100)
    : ReusableVirtualFactory(wrappedGutter, maxSize), VirtualGutter {

    override fun documentChanged(index: Int, node: Node) {
        wrappedGutter.documentChanged(index, node)
    }
}
