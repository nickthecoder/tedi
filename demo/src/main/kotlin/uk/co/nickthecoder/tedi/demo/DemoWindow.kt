package uk.co.nickthecoder.tedi.demo

import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import uk.co.nickthecoder.tedi.*
import uk.co.nickthecoder.tedi.syntax.JavaSyntax
import uk.co.nickthecoder.tedi.syntax.KotlinSyntax
import uk.co.nickthecoder.tedi.ui.*
import java.util.regex.Pattern;

// Note I've deliberately left a trailing semi-colon above, to show that the KotlinSyntax colours are different to
// JavaSyntax (The semicolon should be RED!)

class DemoWindow(stage: Stage = Stage()) {

    val dummyArea = TediArea("dummy")

    /**
     * The root of the scene. top=toolbar, center = tabPane, bottom = findAndReplace
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
     * The non-gui part of find and replace.
     */
    val matcher = TediAreaMatcher(dummyArea)

    /**
     * A tool bar, which appears below the tabPane (inside findAndReplaceToolBars)
     */
    val findBar = FindBar(matcher)

    /**
     * A tool bar, which appears below the findBar (inside findAndReplaceToolBars)
     */
    val replaceBar = ReplaceBar(matcher)

    /**
     * At the bottom of the scene. Contains findBar and replaceBar.
     */
    val findAndReplaceToolBars = VBox()

    // Buttons within the toolBar
    val undo = Button()
    val redo = Button()
    val toggleLineNumbers = ToggleButton()
    val toggleFind = findBar.createToggleButton()
    val toggleFindAndReplace = replaceBar.createToggleButton()
    val goto = GotoDialog.createGotoButton { currentArea }.apply { tooltip = Tooltip("Go to Line (ctrl+G)") }

    val scene = Scene(borderPane, 800.0, 600.0)

    /**
     * Keep track of the "current" TediArea, so that find and replace, and the line-number toggle button
     * affect the correct Control.
     * This is set from within EditorTab (when a the TediArea gains focus).
     *
     * Note, if we were able to close tabs, then closing the last tab should set this back to dummyArea.
     */
    var currentArea: TediArea = dummyArea
        set(v) {
            field.displayLineNumbersProperty().unbindBidirectional(toggleLineNumbers.selectedProperty())

            field = v

            toggleLineNumbers.selectedProperty().bindBidirectional(v.displayLineNumbersProperty())
            toggleLineNumbers.isDisable = false

            // Note, I'm using BetterUndoRedo, and therefore I cannot use TextInputControl.undoableProperty().
            undo.disableProperty().bind(v.undoRedo.undoableProperty.not())
            redo.disableProperty().bind(v.undoRedo.redoableProperty.not())

            matcher.control = v

            // Without the runLater, this doesn't work. I think that's because the new tab is still "kind-of"
            // hidden. Hmm.
            Platform.runLater {
                v.requestFocusOnSceneAvailable()
            }
        }

    init {
        /*
         * Automatically removes children when they are made invisible.
         * Then replaces them if they are made visible again.
         * Without this, the findAndReplaceToolBars VBox would take up space even when its children were hidden.
         */
        RemoveHiddenChildren(findAndReplaceToolBars.children)

        // Applies tedi.css found in tedi-core's jar file.
        TediArea.style(scene)

        with(borderPane) {
            center = tabPane
            top = toolBar
            bottom = findAndReplaceToolBars
        }

        tabPane.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            // NOTE. If we could CLOSE tabs, then we'd also need to handle the case when there are NO
            // open tabs (and therefore newValue == null.
            currentArea = (newValue as EditorTab).tediArea
        }

        // Create some tabs, whose contents are taken from resources within the jar files.
        with(tabPane) {
            tabs.add(EditorTab().apply {
                load(DemoWindow::class.java, "Welcome", false)
                welcomeHighlights()
            })
            tabs.add(EditorTab().apply { load(DemoWindow::class.java, "LICENSE", false) })
            tabs.add(EditorTab().apply {
                // Attach Kotlin syntax highlighting (using the default colour scheme)
                KotlinSyntax.instance.attach(tediArea)
                // Load text from this demo's jar file
                load(DemoWindow::class.java, "Demo", true)
            })
            tabs.add(EditorTab().apply {
                // Attach Kotlin syntax highlighting (using the default colour scheme)
                KotlinSyntax.instance.attach(tediArea)
                // Load text from this demo's jar file
                load(DemoWindow::class.java, "DemoWindow", true)
            })

            tabs.add(EditorTab().apply {
                // Attach JAVA syntax highlighting (using the default colour scheme)
                JavaSyntax.instance.attach(tediArea)
                load(DemoWindow::class.java, "Example", true)
            })

            tabs.add(EditorTab().apply { load(TediArea::class.java, "tedi.css", true) })
        }

        with(undo) {
            loadGraphic(DemoWindow::class.java, "undo.png")
            tooltip = Tooltip("Undo (ctrl+Z)")
            onAction = EventHandler { undo() }
        }

        with(redo) {
            loadGraphic(DemoWindow::class.java, "redo.png")
            tooltip = Tooltip("Redo (ctrl+shift+Z)")
            onAction = EventHandler { redo() }
        }

        with(toggleLineNumbers) {
            loadGraphic(FindBar::class.java, "line-numbers.png")
            tooltip = Tooltip("Show/Hide Line Numbers (ctrl+L)")
        }

        with(toggleFind) {
            tooltip = Tooltip("Find (ctrl+F)")
        }

        with(toggleFindAndReplace) {
            tooltip = Tooltip("Find & Replace (ctrl+R)")
        }

        // Without this, the highlights on the tool bars are wrong.
        with(findBar) {
            toolBar.styleClass.add("bottom")
        }
        with(replaceBar) {
            toolBar.styleClass.add("bottom")
        }

        with(toolBar) {
            items.addAll(undo, redo, toggleLineNumbers, toggleFind, toggleFindAndReplace, goto)
        }

        with(findAndReplaceToolBars) {
            children.addAll(findBar.toolBar, replaceBar.toolBar)
        }

        // Hides the find and replace toolbars.
        matcher.inUse = false

        stage.scene = scene
        with(stage) {
            title = "Tedi Demo Application"
            show()
        }

        // Handle keyboard shortcuts (Hover over buttons etc to see the shortcuts in their tooltips)
        borderPane.addEventFilter(KeyEvent.KEY_PRESSED) { onKeyPressed(it) }

    }

    fun undo() {
        val control = currentArea
        if (control is TediArea) {
            // Note, I'm using BetterUndoRedo, and therefore I cannot use TextInputControl.undo.
            control.undoRedo.undo()
        } else {
            control.undo()
        }
    }

    fun redo() {
        val control = currentArea
        if (control is TediArea) {
            // Note, I'm using BetterUndoRedo, and therefore I cannot use TextInputControl.redo.
            control.undoRedo.redo()
        } else {
            control.redo()
        }
        control.requestFocus()
    }

    /**
     * A quick and dirty keyboard event handler
     */
    fun onKeyPressed(event: KeyEvent) {
        var consume = true

        if (event.isShortcutDown) {
            when (event.code) {
                KeyCode.F -> {
                    matcher.inUse = true
                    findBar.requestFocus()
                }
                KeyCode.Z -> if (event.isShiftDown) redo() else undo()
                KeyCode.Y -> redo()
                KeyCode.G -> GotoDialog(currentArea).show()
                KeyCode.L -> toggleLineNumbers.isSelected = !toggleLineNumbers.isSelected
                KeyCode.R -> {
                    val wasInUse = matcher.inUse
                    replaceBar.toolBar.isVisible = true
                    if (wasInUse) {
                        replaceBar.requestFocus()
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
    inner class EditorTab(
            title: String = "New Document") : Tab() {

        val tediArea = TediArea()

        init {

            content = tediArea
            this.text = title

            with(tediArea) {
                // When selecting "words", this is much better that the default when editing source code.
                wordIterator = SourceCodeWordIterator()

                // Replace the standard undo/redo feature in TextInputControl with a better one.
                // Note, when using this, we cannot use TediArea.undo() etc, and instead use TediArea.undoRedo.undo().
                undoRedo = BetterUndoRedo(tediArea)
            }
            tediArea.addEventFilter(MouseEvent.MOUSE_PRESSED) { contextMenuHandler(it) }
            tediArea.addEventFilter(MouseEvent.MOUSE_RELEASED) { contextMenuHandler(it) }
        }

        fun load(klass: Class<*>, name: String, code: Boolean) {
            val url = klass.getResource(name)
            text = name

            if (url == null) {
                tediArea.text = "Couldn't find resource :\n\n    ${klass.name}.$name"
                return
            }

            try {
                tediArea.text = url.readText()
            } catch (e: Exception) {
                tediArea.text = "Couldn't load resource :\n\n    ${klass.name}.$name"
            }

            if (code) {
                // This will use a monospaced font, and display line numbers.
                tediArea.styleClass.add("code")
            }
        }

        /**
         * A silly example, showing how to get the line, column and character position
         * from a mouse event.
         */
        fun contextMenuHandler(event: MouseEvent) {
            if (event.isPopupTrigger) {
                val pos = tediArea.positionForPoint(event.x, event.y)
                val (line, column) = tediArea.lineColumnFor(pos)
                val lineText = tediArea.getLine(line)

                tediArea.contextMenu = ContextMenu(
                        MenuItem("Mouse : (${event.x.toInt()},${event.y.toInt()})"),
                        MenuItem("Line ${line + 1} Column ${column + 1}"),
                        MenuItem("Character Position $pos"))
                tediArea.contextMenu.items.forEach { it.isDisable = true }

                // Find the word at the given point
                val pattern = Pattern.compile("\\b\\w+?\\b")
                val matcher = pattern.matcher(lineText)
                var word: String? = null
                var wordStart: Int? = null
                var wordEnd: Int? = null
                while (matcher.find()) {
                    if (matcher.start() <= column && matcher.end() >= column) {
                        wordStart = matcher.start()
                        wordEnd = matcher.end()
                        word = lineText.substring(matcher.start(), matcher.end())
                        break
                    }
                }
                if (word != null) {
                    val selectWord = MenuItem("Select word : '$word'")
                    selectWord.setOnAction {
                        tediArea.selectRange(
                                pos - column + wordStart!!,
                                pos - column + wordEnd!!)
                    }
                    tediArea.contextMenu.items.add(selectWord)
                }

                tediArea.contextMenu.show(tediArea, event.screenX, event.screenY)
                event.consume()
            }
        }

        /**
         * A silly example of adding highlights to a document.
         */
        fun welcomeHighlights() {
            val welcome = StyleHighlight("-fx-fill: #008800; -fx-underline: true;")
            // If we had a style sheet containing ".welcome { -fx-text-fill: #004400; }", then we could use:
            // val welcome = StyleClassHighlight("welcome")
            val tedi = FillStyleHighlight("-fx-fill: #0000cc;", "-fx-fill: #ffeeee; ")

            tediArea.highlightRanges().addAll(
                    HighlightRange(0, 7, welcome),
                    HighlightRange(15, 23, tedi)
            )
        }
    }

}
