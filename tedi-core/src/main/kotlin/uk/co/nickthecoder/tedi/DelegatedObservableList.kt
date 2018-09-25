package uk.co.nickthecoder.tedi

import javafx.collections.ModifiableObservableListBase

/**
 * An observable list, delegated to a MutableList.
 *
 * I'm not sure why JavaFX doesn't supply one, surely this is a very common requirement, and
 * is even coded as example here in the :
 * [ModifiableObservableListBase](https://docs.oracle.com/javase/8/javafx/api/javafx/collections/ModifiableObservableListBase.html) Javadocs.
 */
open class DelegatedObservableList<E>(val delegate: MutableList<E>)
    : ModifiableObservableListBase<E>() {

    constructor() : this( mutableListOf())

    override operator fun get(index: Int): E {
        return delegate[index]
    }

    override val size: Int
        get() = delegate.size

    override fun doAdd(index: Int, element: E) {
        delegate.add(index, element)
    }

    override fun doSet(index: Int, element: E): E {
        return delegate.set(index, element)
    }

    override fun doRemove(index: Int): E {
        return delegate.removeAt(index)
    }

}
