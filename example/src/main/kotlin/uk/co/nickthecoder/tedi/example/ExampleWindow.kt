package uk.co.nickthecoder.tedi.example

import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.SplitPane
import javafx.scene.control.TextArea
import javafx.scene.control.ToolBar
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import uk.co.nickthecoder.tedi.CodeWordBreakIterator
import uk.co.nickthecoder.tedi.TediArea
import uk.co.nickthecoder.tedi.ui.GotoDialog

class ExampleWindow(stage: Stage = Stage()) {

    val view1 = TediArea("""package uk.co.nickthecoder.tedi
Hello World
Line 2
Line 3
End""")

    val toolbar = ToolBar()

    val view2 = TediArea(view1)

    val textArea = TextArea("This is a regular TextArea")

    val borderPane = BorderPane()

    val splitPane = SplitPane()

    val scene = Scene(borderPane, 600.0, 400.0)

    init {
        TediArea.style(scene)
        borderPane.center = splitPane
        borderPane.bottom = textArea
        borderPane.top = toolbar

        view1.wordIterator = CodeWordBreakIterator()

        with(toolbar.items) {
            add(createButton("Show Line Numbers") { view1.displayLinesNumbers = true })
            add(createButton("Hide Line Numbers") { view1.displayLinesNumbers = false })
            add(GotoDialog.createGotoButton { view1 })
        }

        // textField.setStyle("-fx-text-fill: green;"); // This works
        // But if I add ".text-field { -fx-text-fill: green; }" into a css file, it doesn't work.
        // Nor if I replace .text-field with .text-input or .text-area

        splitPane.items.addAll(view1, view2)
        stage.scene = scene
        stage.title = "Tedi Example"
        stage.show()
    }

    fun createButton(text: String, action: () -> Unit): Button {
        val button = Button(text)
        button.onAction = EventHandler<ActionEvent> { action() }
        return button
    }
}
