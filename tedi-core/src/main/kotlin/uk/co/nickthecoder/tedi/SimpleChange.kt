package uk.co.nickthecoder.tedi

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
}

class SimpleAddChange<E>(
        list: ObservableList<E>,
        from: Int,
        to: Int)
    : SimpleChange<E>(list, from, to) {

    override fun wasAdded() = true
}

class SimpleRemoveChange<E>(
        list: ObservableList<E>,
        from: Int,
        private val removed: List<E>)

    : SimpleChange<E>(list, from, from) {

    override fun wasRemoved() = true

    override fun getRemoved(): List<E> = removed
}
