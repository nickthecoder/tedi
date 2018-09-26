package uk.co.nickthecoder.tedi.ui

import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.stage.Modality
import javafx.stage.Stage
import uk.co.nickthecoder.tedi.TediArea
import uk.co.nickthecoder.tedi.lineColumnFor
import uk.co.nickthecoder.tedi.loadGraphic
import uk.co.nickthecoder.tedi.positionFor

/**
 * A dialog box, allowing the user to position the caret to a give line.
 * They can also specify a column using the syntax :
 *
 *     line:column
 *
 * Note. the GotoDialog only works with any TextInputControl, including TextArea and TediArea.
 */
class GotoDialog(private var textInputControl: TextInputControl) {

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
        val lineColumn = textInputControl.lineColumnFor(textInputControl.caretPosition)
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

        val position = textInputControl.positionFor(line, column)
        textInputControl.selectRange(position, position)
        stage.hide()
        textInputControl.requestFocus()
    }

    fun onCancel() {
        stage.hide()
    }

    companion object {
        /**
         * Note [textInputContol] is a lambda, which returns a TextInputControl, so that the button can be
         * used in applications where it may need to apply to one of many text fields.
         * e.g. See the DemoWindow, where a single Button is created, which when pressed show a GotoDialog
         * for the current tab's text field.
         */
        @JvmStatic fun createGotoButton(textInputContol: () -> TextInputControl): Button {
            val button = Button()
            button.onAction = EventHandler {
                GotoDialog(textInputContol()).show()
            }

            button.loadGraphic(GotoDialog::class.java, "goto.png")
            return button
        }

        /**
         * Creates a "Goto" button for a single TextInputControl.
         * This is only useful if the button will only be used for a single TextInputControl.
         */
        @JvmStatic fun createGotoButton(textInputControl: TextInputControl) = createGotoButton { textInputControl }
    }
}
