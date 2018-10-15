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

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList

private val EMPTY_PERM = IntArray(0)

abstract class SimpleChange<E>(list: ObservableList<E>, private val from: Int, private val to: Int)

    : ListChangeListener.Change<E>(list) {

    private var hasNext = true

    override fun getFrom() = from

    override fun getTo() = to

    override fun getPermutation() = EMPTY_PERM

    override fun getRemoved(): List<E> = emptyList<E>()

    override fun next(): Boolean {
        val result = hasNext
        hasNext = false
        return result
    }

    override fun reset() {
        hasNext = true
    }
}

class SimpleUpdateChange<E>(
        list: ObservableList<E>,
        from: Int,
        to: Int)

    : SimpleChange<E>(list, from, to) {

    override fun wasUpdated() = true

    override fun toString() = "Change $from..$to"
}

class SimpleAddChange<E>(
        list: ObservableList<E>,
        from: Int,
        to: Int)
    : SimpleChange<E>(list, from, to) {

    override fun wasAdded() = true

    override fun toString() = "Addition $from..$to"
}

class SimpleRemoveChange<E>(
        list: ObservableList<E>,
        from: Int,
        private val removed: List<E>)

    : SimpleChange<E>(list, from, from) {

    override fun wasRemoved() = true

    override fun getRemoved(): List<E> = removed

    override fun toString() = "Remove $removedSize from $from"
}
