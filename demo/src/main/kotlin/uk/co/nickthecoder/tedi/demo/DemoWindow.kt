package uk.co.nickthecoder.tedi.demo

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
import java.net.URL

class DemoWindow(stage: Stage = Stage()) {

    val dummyArea = TediArea("dummy")

    /**
     * The root of the scene. top=toolbar, center = tabPane, bottom = searchAndReplace
     */
    val borderPane = BorderPane()

    /**
     * The tool bar at the top of the scene
     */
    val toolBar = ToolBar()

    /**
     * Contains a set of EditorTabs, which contain the TediViews.
     */
    val tabPane = TabPane()

    /**
     * The non-gui part of search and replace.
     */
    val matcher = TextInputControlMatcher(dummyArea)

    /**
     * A tool bar, which appears below the tabPane (inside searchAndReplaceToolBars)
     */
    val searchBar = SearchBar(matcher)

    /**
     * A tool bar, which appears below the searchBar (inside searchAndReplaceToolBars)
     */
    val replaceBar = ReplaceBar(matcher)

    /**
     * At the bottom of the scene. Contains searchBar and replaceBar.
     */
    val searchAndReplaceToolBars = VBox()

    // Buttons within the toolBar
    val undo = Button()
    val redo = Button()
    val toggleLineNumbers = ToggleButton()
    val toggleSearch = ToggleButton()
    val toggleSearchAndReplace = ToggleButton()
    val goto = GotoDialog.createGotoButton { currentArea }.apply { tooltip = Tooltip("Go to Line (ctrl+G)") }

    val scene = Scene(borderPane, 700.0, 500.0)

    /**
     * Keep track of the "current" TediArea, so that search and replace, and the line-number toggle button
     * affect the correct TediArea.
     * This is set from within EditorTab (when a TediArea gains focus).
     *
     * Note, if we were able to close tabs, then closing the last tab should set this back to dummyArea.
     */
    var currentArea = dummyArea
        set(v) {
            field.displayLineNumbersProperty().unbindBidirectional(toggleLineNumbers.selectedProperty())

            field = v

            toggleLineNumbers.selectedProperty().bindBidirectional(v.displayLineNumbersProperty())
            matcher.textInputControl = v
            undo.disableProperty().bind(v.undoableProperty().not())
            redo.disableProperty().bind(v.redoableProperty().not())
        }

    init {
        /*
         * Automatically removes children when they are made invisible.
         * Then replaces them if they are made visible again.
         * Without this, the searchAndReplaceToolBars VBox would take up space even when its children were hidden.
         */
        RemoveHiddenChildren(searchAndReplaceToolBars.children)

        // Applies tedi.css found in tedi-core's jar file.
        TediArea.style(scene)

        with(borderPane) {
            styleClass.add("example")
            center = tabPane
            top = toolBar
            bottom = searchAndReplaceToolBars
        }

        // Create some tabs, whose contents are taken from resources within the jar files.
        with(tabPane) {
            tabs.add(EditorTab(DemoWindow::class.java.getResource("Welcome")))
            tabs.add(EditorTab(DemoWindow::class.java.getResource("LICENSE")))
            tabs.add(EditorTab(DemoWindow::class.java.getResource("DemoWindow")))
            tabs.add(EditorTab(TediArea::class.java.getResource("tedi.css")))
            // Plus an empty tab
            tabs.add(EditorTab())
        }

        with(undo) {
            loadGraphic(DemoWindow::class.java, "undo.png")
            tooltip = Tooltip("Undo (ctrl+Z)")
            onAction = EventHandler { currentArea.undo() }
        }

        with(redo) {
            loadGraphic(DemoWindow::class.java, "redo.png")
            tooltip = Tooltip("Redo (ctrl+shift+Z)")
            onAction = EventHandler { currentArea.redo() }
        }

        with(toggleLineNumbers) {
            loadGraphic(SearchBar::class.java, "line-numbers.png")
            tooltip = Tooltip("Show/Hide Line Numbers (ctrl+L)")
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

        // Without this, the highlights on the tool bars are wrong.
        with(searchBar) {
            toolBar.styleClass.add("bottom")
        }
        with(replaceBar) {
            toolBar.styleClass.add("bottom")
        }

        with(toolBar) {
            items.addAll(undo, redo, toggleLineNumbers, toggleSearch, toggleSearchAndReplace, goto)
        }

        with(searchAndReplaceToolBars) {
            children.addAll(searchBar.toolBar, replaceBar.toolBar)
        }

        // Hides the search and replace toolbars.
        matcher.inUse = false

        stage.scene = scene
        with(stage) {
            title = "Tedi Demo Application"
            show()
        }

        // Handle keyboard shortcuts (Hover over buttons etc to see the shortcuts in their tooltips)
        borderPane.addEventFilter(KeyEvent.KEY_PRESSED) { onKeyPressed(it) }
    }

    /**
     * A quick and dirty keyboard event handler
     */
    fun onKeyPressed(event: KeyEvent) {
        var consume = true

        if (event.isControlDown) {
            when (event.code) {
                KeyCode.F -> {
                    matcher.inUse = true
                    searchBar.search.requestFocusOnSceneAvailable()
                }
                KeyCode.G -> GotoDialog(currentArea).show()
                KeyCode.L -> toggleLineNumbers.isSelected = !toggleLineNumbers.isSelected
                KeyCode.R -> {
                    val wasInUse = matcher.inUse
                    replaceBar.toolBar.isVisible = true
                    if (wasInUse) {
                        replaceBar.replacement.requestFocusOnSceneAvailable()
                    }
                }
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

    /**
     * A [Tab] within the [tabPane].
     * Contains a TediArea.
     */
    inner class EditorTab(title: String = "New Document") : Tab() {

        constructor(url: URL) : this(url.path.replace(Regex(".*/"), "")) {
            tediArea.text = url.readText()
        }

        val tediArea = TediArea()

        init {
            content = tediArea
            text = title

            // When selecting "words", this is much better that the default when editing source code.
            tediArea.wordIterator = CodeWordBreakIterator()

            // Make toggleLineNumbers button and the matcher refer this this tediArea whenever it gains focus.
            tediArea.focusedProperty().addListener { _, _, newValue ->
                if (newValue == true) {
                    currentArea = tediArea
                }
            }
            // Select the TediArea when this tab is selected
            selectedProperty().addListener { _, _, newValue ->
                if (newValue == true) {
                    tediArea.requestFocusOnSceneAvailable()
                }
            }

        }

    }
}
