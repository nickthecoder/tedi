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
package uk.co.nickthecoder.tedi.ui

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.Node

/**
 * Takes a list of child nodes, and observes when items are add/removed, keeping an internal copy of that list.
 *
 * When one of the children become invisible, they are automatically removed from the original list.
 * When a child is made visible again, it is added back to the original list.
 *
 * The tricky part is deciding WHERE in the list to put back the child.
 * We cannot use the original index (as the list may have shrunk).
 *
 * For Panes (such as VBox and HBox), [children] should be Pane.getChildren().
 * For ToolBar, it is ToolBar.getItems().
 *
 * Note. Items should be added while VISIBLE.
 *
 * Note. This hasn't been exhaustively tested, but doesn't fail in my example application. YMMV.
 *
 */
open class RemoveHiddenChildren(
        val children: ObservableList<Node>) {

    protected val allChildren = mutableListOf<Node>()

    protected val ignoreRemovals = mutableListOf<Node>()

    protected val visibilityListeners = mutableListOf<VisibilityListener>()

    protected val childChangeListener = ListChangeListener<Node> { change ->
        childrenChanged(change)
    }

    init {
        children.addListener(childChangeListener)
    }

    protected fun childrenChanged(change: ListChangeListener.Change<out Node>) {
        while (change.next()) {

            if (change.wasAdded()) {
                change.addedSubList.forEachIndexed { i, child ->
                    val index = change.from + i
                    // If the child has already been added, then ignore it.
                    // It was added by THIS, when it was made visible again.
                    if (!allChildren.contains(child)) {
                        rememberChild(child, index)
                    }
                }
            }
            if (change.wasRemoved()) {
                change.removed.forEach { child ->
                    if (ignoreRemovals.contains(child)) {
                        // The child was removed by THIS because it became invisible
                        ignoreRemovals.remove(child)
                    } else {
                        // Removed externally (not by THIS), therefore forget about it.
                        allChildren.remove(child)
                        visibilityListeners.removeAll(visibilityListeners.filter { it.node == child })
                    }
                }
            }
        }

        //println("allChildren : $allChildren")
        //println("   children : $children")
    }

    protected fun rememberChild(child: Node, index: Int) {

        val myIndex = if (index > 0) {
            // Look at the PREVIOUS child
            val found = findMyIndex(children[index - 1])
            if (found >= 0) {
                // Place it AFTER the found child
                found + 1
            } else {
                allChildren.size
            }
        } else {
            if (index < children.size - 1) {
                // Look at the NEXT child
                val found = findMyIndex(children[index + 1])
                if (found > 1) {
                    // Place it BEFORE the found child
                    found - 1
                } else {
                    0
                }
            } else {
                0
            }
        }
        allChildren.add(myIndex, child)
        visibilityListeners.add(VisibilityListener(child))
    }

    protected fun insertIndex(child: Node): Int {
        val index = allChildren.indexOf(child)

        for (i in index - 1 downTo 0) {
            val node = allChildren[i]
            val before = children.indexOf(node)
            if (before >= 0) return before + 1
        }

        for (i in index + 1..allChildren.size - 1) {
            val node = allChildren[i]
            val after = children.indexOf(node)
            if (after >= 0) return after - 1
        }

        return children.size
    }

    protected fun findMyIndex(node: Node): Int {
        allChildren.forEachIndexed { i, c ->
            if (c == node) {
                return i
            }
        }
        return -1
    }

    inner class VisibilityListener(val node: Node) : ChangeListener<Boolean> {

        init {
            node.visibleProperty().addListener(this)
        }

        override fun changed(observable: ObservableValue<out Boolean>?, oldValue: Boolean?, newValue: Boolean?) {
            if (newValue == true) {

                children.add(insertIndex(node), node)
            } else {
                ignoreRemovals.add(node)
                children.remove(node)
            }
        }
    }
}