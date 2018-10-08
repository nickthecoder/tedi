package uk.co.nickthecoder.tedi.util

import javafx.scene.Node

/**
 * Creates nodes for [VirtualView]
 */
interface VirtualFactory {

    /**
     * Creates a node for one item of [VirtualView]'s list
     * (i.e. a node for a Paragraphs when used by TediArea)
     * The node contains a single line number (or whatever data is required by your gutter).
     *
     * NOTE. The nodes should be simple. Have not focusable nodes, such as Button.
     * Also, the node must return a valid value from prefWidth( height ) as soon as it is created, and
     * added to the scene (BEFORE a layout it performed).
     * I think this means that the use of HBox and similar controls cannot be used. Sorry.
     * [BoxGutter] creates a HBox-like nodes, so that may be a good place to start.
     */
    fun createNode(index: Int): Node

    /**
     * When nodes are no longer visible, they are removed from the scene graph.
     * Use this method to tidy up (remove listeners for example).
     * If you want to re-use nodes, then add them to your cache when [free] is called, and then
     * remove them from the cache from within [createNode].
     *
     * NOTE, this is called just BEFORE the node is removed from the scene graph.
     */
    fun free(index: Int, node: Node) {}

    /**
     * Called whenever an item in [VirtualView]'s list changes
     * (for example, when adding/removing text within a Paragraph).
     */
    fun itemChanged(index: Int, node: Node) {}

}
