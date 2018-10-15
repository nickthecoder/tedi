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
