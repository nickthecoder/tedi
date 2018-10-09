package uk.co.nickthecoder.tedi.util

import javafx.scene.Node

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
    fun documentChanged(index: Int, node: Node);

}
