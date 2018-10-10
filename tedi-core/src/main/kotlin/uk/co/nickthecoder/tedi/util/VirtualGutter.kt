package uk.co.nickthecoder.tedi.util

import javafx.scene.Node
import javafx.scene.layout.Region

/**
 * Creates [Node]s to the left of [VirtualView], typically used to display line numbers.
 *
 * The nodes are virtual, i.e. only the visible items are created.
 *
 * While the primary use case is for line numbers, a gutter can be useful for other purposes. For example :
 *
 * - Click to mark the line as a "Favourite", then elsewhere in the GUI jump to any of the favourite places.
 * - A button to mark lines as "important" (You could use a HighlightRanges to highlight the line too)
 * - Colour the gutter with information from git (lines added, lines modified etc)
 * - Add other meta-data, such as who is responsible for that part of the document, signed off by etc.
 * - Controls for expand/contract for a folding editor
 *   (though this would probably require additional support from TediArea itself)
 * - Controls for manipulating the whole line (e.g. delete a line/block with a single click).
 * - Adding "check marks" to lines, which can then be processed from other parts of your GUI.
 *   Similar to ticking emails, and then clicking a button to mark them all as read.
 *
 * I suggest extending [LineNumberGutter], if you want to add extra features to your gutters.
 *
 * I'm not happy that this is a class, and not an interface. However, I want it to be styleable
 */
abstract class VirtualGutter : Region(), VirtualFactory {

    /**
     * Called whenever ANY changes are made to VirtualView's list, i.e.,
     * insertions, deletions and updates.
     *
     * This solely designed to allow line numbers to be updated when Paragraphs are added/deleted.
     *
     * Note, this is called once per visible node.
     *
     * @param index An index into VirtualView's list (i.e. the Paragraph's index for TediArea's gutter).
     *
     * @param node The corresponding node created earlier via [createNode]
     */
    abstract fun documentChanged(index: Int, node: Node)

    public override fun getChildren() = super.getChildren()

    init {
        styleClass.add("gutter")
        isManaged = false
    }

    // Children are being manually laid out, so do nothing here.
    override fun layoutChildren() {
        return
    }
}

abstract class ReusableVirtualGutter(private val maxSize: Int = 100) : VirtualGutter() {

    private val reusableList = mutableListOf<Node>()

    override fun createNode(index: Int): Node = if (reusableList.isEmpty()) {
        createNewNode(index)
    } else {
        reusableList.removeAt(reusableList.size - 1).apply {
            if (this is UpdatableNode) {
                update(index)
            }
        }
    }

    abstract fun createNewNode(index: Int): Node

    override fun free(index: Int, node: Node) {
        if (reusableList.size < maxSize) {
            reusableList.add(node)
        }
    }

}