package uk.co.nickthecoder.tedi.example

import javafx.scene.Scene
import javafx.scene.control.SplitPane
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import uk.co.nickthecoder.tedi.TediArea

class ExampleWindow(stage: Stage = Stage()) {

    val view1 = TediArea("""package uk.co.nickthecoder.tedi
Hello World
Line 2
Line 3
End""")

    val view2 = TediArea(view1.content)

    val borderPane = BorderPane()

    val splitPane = SplitPane()

    val scene = Scene(borderPane, 600.0, 400.0)

    init {
        TediArea.style(scene)
        borderPane.center = splitPane
        splitPane.items.addAll(view1, view2)
        stage.scene = scene
        stage.title = "Tedi Example"
        stage.show()
    }
    
}
