package uk.co.nickthecoder.tedi

import javafx.beans.property.ListProperty
import javafx.beans.property.ListPropertyBase
import javafx.beans.property.ObjectPropertyBase
import javafx.beans.property.Property
import javafx.collections.ModifiableObservableListBase

class TediDocument(text: String = "") {

    internal val lines = DelegatedStringList()

    internal val linesProperty: ListProperty<String> = object : ListPropertyBase<String>(lines) {
        override fun getBean() = this
        override fun getName() = "linesProperty"
    }

    inner class TediTextProperty : ObjectPropertyBase<String>() {
        override fun getBean() = text
        override fun getName() = "text"
        internal fun textChanged() {
            fireValueChangedEvent()
        }
    }

    val textProperty: Property<String> = TediTextProperty()

    var text: String
        get() = lines.joinToString(separator = "\n")
        set(v) {
            lines.clear()
            lines.addAll(v.split("\n"))
            (textProperty as TediTextProperty).textChanged()
        }

    init {
        this.text = text
    }

    fun lines(): List<String> = lines

    internal class DelegatedStringList : ModifiableObservableListBase<String>() {

        private val delegate = mutableListOf<String>()

        override val size: Int
            get() = delegate.size

        override fun doAdd(index: Int, element: String) {
            delegate.add(index, element)
        }

        override fun doRemove(index: Int) = delegate.removeAt(index)

        override fun doSet(index: Int, element: String) = delegate.set(index, element)

        override fun get(index: Int) = delegate[index]
    }
}
