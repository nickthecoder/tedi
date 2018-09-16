package uk.co.nickthecoder.tedi.example

import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.SplitPane
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

    val borderPane = BorderPane()

    val splitPane = SplitPane()

    val scene = Scene(borderPane, 600.0, 400.0)

    init {
        TediArea.style(scene)

        with(borderPane) {
            styleClass.add("example")
            center = splitPane
            top = toolbar
        }

        view1.wordIterator = CodeWordBreakIterator()

        with(toolbar.items) {
            add(createButton("Show Line Numbers") { view1.displayLineNumbers = true })
            add(createButton("Hide Line Numbers") { view1.displayLineNumbers = false })
            add(GotoDialog.createGotoButton { view1 })
        }

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
