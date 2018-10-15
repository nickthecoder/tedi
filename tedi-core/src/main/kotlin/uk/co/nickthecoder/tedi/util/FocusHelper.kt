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
import javafx.scene.Parent

/**
 * JavaFX 8 does not expose an API to navigate focus. This is a "bodge" to get around the problem.
 * Attempts to request focus on the next node.
 *
 * Note, the ordering is based on order of the children within [Parent] nodes, and may not be the
 * same order as JavaFX's normal navigation.
 *
 * @param attempts - In case there is a bug, prevent infinite loops by stopping after checking this many nodes.
 *
 * I've added this as a github gist :
 * https://gist.github.com/nickthecoder/57ac20829214f1209e41e851d955ed35
 */
fun Node.focusNext(attempts: Int = 1000) = FocusNext(this, attempts).start()

/**
 * JavaFX 8 does not expose an API to navigate focus. This is a "bodge" to get around the problem.
 * Attempts to request focus on the previous node.
 *
 * Note, the ordering is based on order of the children within [Parent] nodes, and may not be the
 * same order as JavaFX's normal navigation.
 *
 * @param attempts - In case there is a bug, prevent infinite loops by stopping after checking this many nodes.
 */
fun Node.focusPrevious(attempts: Int = 1000) = FocusPrevious(this, attempts).start()


/**
 * Look at all later siblings one at a time.
 * If it isFocusTraversable and visible and not disabled, then we are done!.
 * If it is a parent, visible and not disabled, then we also need to check its
 * children (recursively) before moving onto the next sibling.
 *
 * If we reach the end of the later siblings, then we need to look to our parent,
 * and check its later siblings (in the same manner, recursively into its children).
 * Again, if we find one that is focusTraversable, visible and not disabled, then we are done.
 *
 * If we reach the top-most parent, without success, then we need to start
 * from its first child, working forwards as before.
 * Note. The first child will not have been checked yet, because so far, we have only been
 * checking later siblings and their descendants.
 *
 * Note. It is quite possible to loop around the whole scene tree, and end  up where we started.
 * In which case we stop without doing anything.
 *
 * Also, in case this code is buggy, if we check too many nodes (the default is 1000), then
 * we give up.
 *
 * All methods return true on success or the max attempts is exceeded,
 * and false if further searching is required..
 */

private class FocusNext(val startNode: Node, var attempts: Int) {

    fun tryAllChildren(parent: Parent): Boolean {
        if (--attempts <= 0) return true

        for (child in parent.childrenUnmodifiable) {
            if (tryOneNodeRecursively(child)) {
                return true
            }
        }

        return false
    }


    fun tryOneNodeRecursively(node: Node): Boolean {
        if (--attempts <= 0) return true
        if (node === startNode) {
            attempts = 0
            return true
        }

        if (node.isFocusTraversable && node.isVisible && !node.isDisabled) {
            node.requestFocus()
            return true
        }
        if (node is Parent && node.isVisible && !node.isDisabled) {
            if (tryAllChildren(node)) {
                return true
            }
        }
        return false
    }

    /**
     *
     * Only looks at later siblings from [node].
     */
    fun tryFromNode(node: Node): Boolean {
        val parent = node.parent
        parent ?: return false

        val children = parent.childrenUnmodifiable
        val idx = children.indexOf(node)
        if (idx >= 0) {
            for (i in idx + 1..children.size - 1) {
                if (tryOneNodeRecursively(children[i])) {
                    return true
                }
            }
        }

        // Now look at PARENT's later siblings
        if (tryFromNode(parent)) {
            return true
        }

        return false
    }


    fun start(): Boolean {
        if (tryFromNode(startNode)) {
            return true
        }

        var topMost = startNode
        while (topMost.parent != null) {
            topMost = topMost.parent
        }
        if (topMost is Parent) {
            return tryAllChildren(topMost)
        }
        return false
    }
}

/**
 * This is the same as FocusNext, except it looks at EARLIER siblings.
 */
private class FocusPrevious(val startNode: Node, var attempts: Int) {

    fun tryAllChildren(parent: Parent): Boolean {
        if (--attempts <= 0) return true

        for (child in parent.childrenUnmodifiable.reversed()) {
            if (tryOneNodeRecursively(child)) {
                return true
            }
        }

        return false
    }

    // This method is identical in both classes. Should I create a base class?
    fun tryOneNodeRecursively(node: Node): Boolean {
        if (--attempts <= 0) return true
        if (node === startNode) {
            attempts = 0
            return true
        }

        if (node.isFocusTraversable && node.isVisible && !node.isDisabled) {
            node.requestFocus()
            return true
        }
        if (node is Parent && node.isVisible && !node.isDisabled) {
            if (tryAllChildren(node)) {
                return true
            }
        }
        return false
    }

    fun tryFromNode(node: Node): Boolean {
        val parent = node.parent
        parent ?: return false

        val children = parent.childrenUnmodifiable
        val idx = children.indexOf(node)
        if (idx >= 1) {
            for (i in idx - 1 downTo 0) {
                if (tryOneNodeRecursively(children[i])) {
                    return true
                }
            }
        }

        // Now look at PARENT's earlier siblings
        if (tryFromNode(parent)) {
            return true
        }

        return false
    }

    fun start(): Boolean {
        if (tryFromNode(startNode)) {
            return true
        }

        var topMost = startNode
        while (topMost.parent != null) {
            topMost = topMost.parent
        }
        if (topMost is Parent) {
            return tryAllChildren(topMost)
        }
        return false
    }
}
