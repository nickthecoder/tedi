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
import uk.co.nickthecoder.tedi.ui.*
import java.util.regex.Pattern

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
    val matcher = TextInputControlMatcher(dummyArea)

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

    val scene = Scene(borderPane, 700.0, 500.0)

    /**
     * Keep track of the "current" TextArea/TediArea, so that find and replace, and the line-number toggle button
     * affect the correct Control.
     * This is set from within EditorTab (when a the TextInputControl gains focus).
     *
     * Note, if we were able to close tabs, then closing the last tab should set this back to dummyArea.
     */
    var currentArea: TextInputControl = dummyArea
        set(v) {
            val oldValue = field
            if (oldValue is TediArea) {
                oldValue.displayLineNumbersProperty().unbindBidirectional(toggleLineNumbers.selectedProperty())
            }

            field = v

            if (v is TediArea) {
                toggleLineNumbers.selectedProperty().bindBidirectional(v.displayLineNumbersProperty())
                toggleLineNumbers.isDisable = false
                // Note, I'm using BetterUndoRedo, and therefore I cannot use TextInputControl.undoableProperty().
                undo.disableProperty().bind(v.undoRedo.undoableProperty.not())
                redo.disableProperty().bind(v.undoRedo.redoableProperty.not())
            } else {
                toggleLineNumbers.isSelected = false
                toggleLineNumbers.isDisable = true
                undo.disableProperty().bind(v.undoableProperty().not())
                redo.disableProperty().bind(v.redoableProperty().not())
            }

            matcher.textInputControl = v

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
            styleClass.add("example")
            center = tabPane
            top = toolBar
            bottom = findAndReplaceToolBars
        }

        // Create some tabs, whose contents are taken from resources within the jar files.
        with(tabPane) {
            tabs.add(TediTab().apply { load(DemoWindow::class.java, "Welcome", false) })
            tabs.add(TediTab().apply { load(DemoWindow::class.java, "LICENSE", false) })
            tabs.add(TediTab().apply { load(DemoWindow::class.java, "Demo", true) })
            tabs.add(TediTab().apply { load(DemoWindow::class.java, "DemoWindow", true) })
            tabs.add(TediTab().apply { load(TediArea::class.java, "tedi.css", true) })

            tabs.add(TextAreaTab().apply { load(DemoWindow::class.java, "TextArea", false) })
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
    abstract inner class EditorTab(
            val textInput: TextInputControl,
            title: String = "New Document") : Tab() {

        init {
            content = textInput
            this.text = title

            // Select the TediArea when this tab is selected
            selectedProperty().addListener { _, _, newValue ->
                if (newValue == true) {
                    textInput.requestFocusOnSceneAvailable()
                }
            }

            // Make toggleLineNumbers button and the matcher refer this this textInput whenever it gains focus.
            textInput.focusedProperty().addListener { _, _, newValue ->
                if (newValue == true) {
                    currentArea = textInput
                }
            }
        }

        fun load(klass: Class<*>, name: String, code: Boolean) {
            val url = klass.getResource(name)
            text = name

            if (url == null) {
                textInput.text = "Couldn't find resource :\n\n    ${klass.name}.$name"
                return
            }

            try {
                textInput.text = url.readText()
            } catch (e: Exception) {
                textInput.text = "Couldn't load resource :\n\n    ${klass.name}.$name"
            }

            if (code) {
                // This will use a monospaced font, and display line numbers.
                textInput.styleClass.add("code")
            }
        }
    }

    inner class TediTab
        : EditorTab(TediArea()) {

        val tediArea = textInput as TediArea

        init {
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
    }

    inner class TextAreaTab : EditorTab(TextArea(), "TextArea")

}
