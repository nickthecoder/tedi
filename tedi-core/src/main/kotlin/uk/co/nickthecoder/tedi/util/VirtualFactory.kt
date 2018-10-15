/*
Tedi
Copyright (C) 2018 Nick Robinson

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2 only, as
published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
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
     * NOTE. The nodes should have no focusable items.
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
