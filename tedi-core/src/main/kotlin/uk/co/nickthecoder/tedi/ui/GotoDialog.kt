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

import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.stage.Modality
import javafx.stage.Stage
import uk.co.nickthecoder.tedi.TediArea
import uk.co.nickthecoder.tedi.util.loadGraphic

/**
 * A dialog box, allowing the user to position the caret to a give line.
 * They can also specify a column using the syntax :
 *
 *     line:column
 *
 * Note. the GotoDialog only works with any TextInputControl, including TextArea and TediArea.
 */
class GotoDialog(private var tediArea: TediArea) {

    val label = Label("Line [:Column]")
    val field = TextField()

    val hBox = HBox()

    val borderPane = BorderPane()

    val ok = Button("Ok")
    val cancel = Button("Cancel")

    val buttons = FlowPane()
    val scene = Scene(borderPane)
    val stage = Stage()

    init {
        with(borderPane) {
            center = hBox
            bottom = buttons
        }

        with(ok) {
            onAction = EventHandler { onOk() }
            isDefaultButton = true
        }

        with(cancel) {
            isCancelButton = true
            onAction = EventHandler { onCancel() }
        }

        with(buttons) {
            styleClass.add("tedi-buttons")
            children.addAll(ok, cancel)
        }

        with(hBox) {
            styleClass.add("tedi-form")
            children.addAll(label, field)
        }

        // Initial value is the current position.
        val lineColumn = tediArea.lineColumnForPosition(tediArea.caretPosition)
        field.text = "${lineColumn.first + 1}:${lineColumn.second + 1}"

        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "Go to Line/Column"
        stage.scene = scene

        TediArea.style(scene)
    }


    fun show() {
        stage.show()
        stage.centerOnScreen()
    }

    fun onOk() {
        val split = field.text.split(":")
        val line = split[0].toInt() - 1
        val column = if (split.size > 1) (split[1].toInt() - 1) else 0

        val position = tediArea.positionOfLine(line, column)
        tediArea.selectRange(position, position)
        stage.hide()
        tediArea.requestFocus()
    }

    fun onCancel() {
        stage.hide()
    }

    companion object {
        /**
         * Note [tediArea] is a lambda, which returns a TediArea, so that the button can be
         * used in applications where it may need to apply to one of many TediAreas.
         * e.g. See the DemoWindow, where a single Button is created, which when pressed show a GotoDialog
         * for the current tab's text field.
         */
        @JvmStatic fun createGotoButton(tediArea: () -> TediArea): Button {
            val button = Button()
            button.onAction = EventHandler {
                GotoDialog(tediArea()).show()
            }

            button.loadGraphic(GotoDialog::class.java, "goto.png")
            return button
        }

        /**
         * Creates a "Goto" button for a single TediArea.
         * This is only useful if the button will only be used for a single TediArea.
         */
        @JvmStatic fun createGotoButton(tediArea: TediArea) = createGotoButton { tediArea }
    }
}
