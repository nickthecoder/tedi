package uk.co.nickthecoder.tedi.example

import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import uk.co.nickthecoder.tedi.CodeWordBreakIterator
import uk.co.nickthecoder.tedi.TediArea
import uk.co.nickthecoder.tedi.loadGraphic
import uk.co.nickthecoder.tedi.requestFocusOnSceneAvailable
import uk.co.nickthecoder.tedi.ui.*

class ExampleWindow(stage: Stage = Stage()) {

    val view1 = TediArea("""package uk.co.nickthecoder.tedi
Hello World
Line 2
Line 3
End
""" + "1\n2\n3\n4\n5\n6\n7\n8\n9\n10\nWeee !!!\n".repeat(10))

    val toolbar = ToolBar()

    val view2 = TediArea(view1)

    val borderPane = BorderPane()

    val splitPane = SplitPane()

    val scene = Scene(borderPane, 600.0, 400.0)

    val matcher = TextInputControlMatcher(view1)
    val searchBar = SearchBar(matcher)
    val replaceBar = ReplaceBar(matcher)

    val searchAndReplaceToolBars = VBox()

    val toggleLineNumbers = ToggleButton()
    val toggleSearch = ToggleButton()
    val toggleSearchAndReplace = ToggleButton()

    init {
        /**
         * Auto removes and replaces children when they are made invisible.
         * Without this, the VBox would take up space even when its children were hidden.
         */
        RemoveHiddenChildren(searchAndReplaceToolBars.children)

        TediArea.style(scene)

        with(borderPane) {
            styleClass.add("example")
            center = splitPane
            top = toolbar
            bottom = searchAndReplaceToolBars
        }

        view1.wordIterator = CodeWordBreakIterator()

        with(toggleLineNumbers) {
            loadGraphic(SearchBar::class.java, "line-numbers.png")
            tooltip = Tooltip("Show/Hide Line Numbers (ctrl+L)")
            selectedProperty().bindBidirectional(view1.displayLineNumbersProperty())
        }

        with(toggleSearch) {
            loadGraphic(SearchBar::class.java, "search.png")
            tooltip = Tooltip("Find (ctrl+F)")
            selectedProperty().bindBidirectional(searchBar.toolBar.visibleProperty())
        }

        with(toggleSearchAndReplace) {
            loadGraphic(ReplaceBar::class.java, "replace.png")
            tooltip = Tooltip("Find & Replace (ctrl+R)")
            selectedProperty().bindBidirectional(replaceBar.toolBar.visibleProperty())
        }

        with(toolbar.items) {
            add(toggleLineNumbers)
            add(toggleSearch)
            add(toggleSearchAndReplace)
            add((GotoDialog.createGotoButton { view1 }).apply { tooltip = Tooltip("Go to Line (ctrl+G)") })
        }

        with(searchAndReplaceToolBars) {
            children.addAll(searchBar.toolBar, replaceBar.toolBar)
        }

        // Hides the search and replace toolbars.
        matcher.inUse = false

        splitPane.items.addAll(view1, view2)
        stage.scene = scene
        stage.title = "Tedi Demo Application"
        stage.show()
        borderPane.addEventFilter(KeyEvent.KEY_PRESSED) { onKeyPressed(it) }
    }

    /**
     * A quick and dirty keyboard event handler
     */
    fun onKeyPressed(event: KeyEvent) {
        var consume = true

        if (event.isControlDown) {
            when (event.code) {
                KeyCode.F -> matcher.inUse = true
                KeyCode.R -> {
                    val wasInUse = matcher.inUse
                    replaceBar.toolBar.isVisible = true
                    if (wasInUse) {
                        replaceBar.replacement.requestFocusOnSceneAvailable()
                    }
                }
                KeyCode.G -> GotoDialog(view1).show()
                else -> consume = false
            }

        } else {
            // Not control down
            when (event.code) {
                KeyCode.ESCAPE -> onEscape()
                else -> consume = false
            }
        }

        if (consume) {
            event.consume()
        }
    }

    fun onEscape() {
        matcher.inUse = false
    }

    fun createButton(text: String, action: () -> Unit): Button {
        val button = Button(text)
        button.onAction = EventHandler<ActionEvent> { action() }
        return button
    }
}
