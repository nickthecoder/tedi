package uk.co.nickthecoder.tedi.ui

import javafx.beans.value.ChangeListener
import javafx.scene.control.TextInputControl

/**
 * The Tedi project is really all about TediArea, but it seemed a shame to write a nice find and replace
 * utility that didn't work for regular TextAreas.
 * So [TextInputControlMatcher] can work with with TextAreas, or any other control that extends
 * [TextInputControl].
 *
 * Difference between this and [TediAreaMatcher] :
 *
 * - No highlighting of matches
 * - Restarts matching whenever the text changes (because the match start and end positions would
 *   become invalid as soon as text is inserted or deleted).
 */
open class TextInputControlMatcher(textInputControl: TextInputControl)

    : AbstractMatcher<TextInputControl>(textInputControl) {

    protected var textChangedListener = ChangeListener<String> { _, _, _ -> textChanged() }

    init {
        textInputControl.textProperty().addListener(textChangedListener)
    }

    override fun controlChanged(oldValue: TextInputControl?, newValue: TextInputControl?) {
        super.controlChanged(oldValue, newValue)
        oldValue?.textProperty()?.removeListener(textChangedListener)
        newValue?.textProperty()?.addListener(textChangedListener)
    }

    open fun textChanged() {
        startFind()
    }
}
