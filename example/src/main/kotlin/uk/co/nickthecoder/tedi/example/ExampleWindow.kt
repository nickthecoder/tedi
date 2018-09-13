package uk.co.nickthecoder.tedi.example

import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import uk.co.nickthecoder.tedi.TediDocument
import uk.co.nickthecoder.tedi.TediView

class ExampleWindow(stage: Stage = Stage()) {

    val document = TediDocument("""Hello World
Line 2
Line 3
End""")

    val view = TediView(document)

    val borderPane = BorderPane()

    val scene = Scene(borderPane, 600.0, 400.0)

    init {
        borderPane.center = view
        stage.scene = scene
        stage.title = "Tedi Example"
        stage.show()

        println("Document's text = ${document.text}")
        println("View's text = ${view.text}")

        document.text = "Hello\nWorld\nLast line"

        println("Document's text = ${document.text}")
        println("View's text = ${view.text}")

    }

}
