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
 */
abstract class ReusableVirtualFactory<N>(val maxSize: Int = 100)
    : VirtualFactory where N : Node, N : UpdatableNode {

    val reusableList = mutableListOf<N>()

    override fun createNode(index: Int): N = if (reusableList.isEmpty()) {
        createNewNode(index)
    } else {
        reusableList.removeAt(reusableList.size - 1).apply { update(index) }
    }

    abstract fun createNewNode(index: Int): N

    override fun free(index: Int, node: Node) {
        if (reusableList.size < maxSize) {
            @Suppress("UNCHECKED_CAST")
            reusableList.add(node as N)
        }
    }
}
