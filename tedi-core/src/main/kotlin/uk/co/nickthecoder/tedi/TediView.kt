package uk.co.nickthecoder.tedi

import com.sun.javafx.binding.ExpressionHelper
import javafx.beans.InvalidationListener
import javafx.beans.value.ChangeListener
import javafx.scene.control.TextInputControl

class TediView(val document: TediDocument)

    : TextInputControl(TediViewContent(document)) {

    constructor() : this(TediDocument())

    init {
        styleClass.addAll("text-area", "tedi-view")
        document.textProperty.bind(textProperty())
    }

    override fun createDefaultSkin() = TediSkin(this)

    class TediViewContent(val document: TediDocument) : TextInputControl.Content {

        private var helper: ExpressionHelper<String>? = null

        override fun addListener(changeListener: ChangeListener<in String>) {
            helper = ExpressionHelper.addListener(helper, this, changeListener)
        }

        override fun removeListener(changeListener: ChangeListener<in String>) {
            helper = ExpressionHelper.removeListener(helper, changeListener)
        }

        override fun getValue(): String {
            return get()
        }

        override fun addListener(listener: InvalidationListener) {
            helper = ExpressionHelper.addListener(helper, this, listener)
        }

        override fun removeListener(listener: InvalidationListener) {
            helper = ExpressionHelper.removeListener(helper, listener)
        }

        override fun get() = document.text

        override fun delete(start: Int, end: Int, notifyListeners: Boolean) {
            val oldText = document.text
            document.text = oldText.substring(0, start) + oldText.substring(end)
        }

        override fun get(start: Int, end: Int): String {
            return document.text.substring(start, end)
        }

        override fun insert(index: Int, text: String, notifyListeners: Boolean) {
            val oldText = document.text
            document.text = oldText.substring(0, index) + text + oldText.substring(index)
            //TODO notify listeners???
        }

        override fun length() = document.text.length

    }

}
