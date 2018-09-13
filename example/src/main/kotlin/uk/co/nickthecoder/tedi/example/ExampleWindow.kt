package uk.co.nickthecoder.tedi.example

import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import uk.co.nickthecoder.tedi.TediArea

class ExampleWindow(stage: Stage = Stage()) {

    val view = TediArea("""Hello World
Line 2
Line 3
End""")

    val borderPane = BorderPane()

    val scene = Scene(borderPane, 600.0, 400.0)

    init {
        borderPane.center = view
        stage.scene = scene
        stage.title = "Tedi Example"
        stage.show()
    }

}
