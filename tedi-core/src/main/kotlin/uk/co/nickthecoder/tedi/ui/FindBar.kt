package uk.co.nickthecoder.tedi.ui

import com.sun.javafx.collections.ObservableListWrapper
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import uk.co.nickthecoder.tedi.loadGraphic
import uk.co.nickthecoder.tedi.onSceneAvailable
import uk.co.nickthecoder.tedi.requestFocusWithCaret

/**
 * An example GUI for use with [TextInputControlMatcher] or [TediAreaMatcher].
 *
 * Add [toolBar] to your scene.
 * Making the [toolBar] hidden (toolbar.isVisible = false) will disable the [matcher].
 *
 * If your application has multiple TediAreas (for example inside a TabPane), then you can either :
 * 1. Create a single matcher, and a single [FindBar] for uses
 * 2. Create a matcher and a [FindBar] for each TediArea.
 *
 * If doing the former, then you need to set the TextInputControlMatcher.textInputControl appropriately
 * (for example, whenever you select a new tab, or whenever a TediArea gains focus).
 */
open class FindBar(val matcher: AbstractMatcher<*>) {

    val toolBar = ToolBar()

    val find = HistoryComboBox(findHistory)

    val prev = Button()

    val next = Button()

    val matchCase = CheckBox("_Match Case")

    val matchRegex = CheckBox("R_egex")

    val matchWords = CheckBox("_Words")

    val status = Label()

    init {

        find.isEditable = true

        toolBar.visibleProperty().addListener { _, _, newValue ->
            if (newValue == true) {
                requestFocus()
            }
        }

        matcher.inUseProperty.bindBidirectional(toolBar.visibleProperty())

        toolBar.isVisible = true

        with(toolBar) {
            styleClass.add("tedi-find-bar")
            items.addAll(find, prev, next, Separator(), matchCase, matchRegex, matchWords, Separator(), status)
            addEventFilter(KeyEvent.KEY_PRESSED) { keyPressed(it) }
        }

        with(find) {
            promptText = "find"
            styleClass.add("find")
            editor.textProperty().bindBidirectional(matcher.findProperty)
        }

        with(prev) {
            styleClass.add("prev")
            disableProperty().bind(matcher.hasPrevProperty.not())
            loadGraphic(FindBar::class.java, "prev.png")
            tooltip = Tooltip("Previous match (Up)")
            onAction = EventHandler { matcher.previousMatch() }
        }

        with(next) {
            styleClass.add("next")
            disableProperty().bind(matcher.hasNextProperty.not())
            loadGraphic(FindBar::class.java, "next.png")
            tooltip = Tooltip("Next match (Down)")
            onAction = EventHandler { matcher.nextMatch() }
        }

        with(matchCase) {
            isMnemonicParsing = true
            styleClass.add("match-case")
            tooltip = Tooltip("(ctrl+M)")
            selectedProperty().bindBidirectional(matcher.matchCaseProperty)
        }

        with(matchRegex) {
            styleClass.add("match-regex")
            tooltip = Tooltip("(ctrl+E)")
            selectedProperty().bindBidirectional(matcher.matchRegexProperty)
        }

        with(matchWords) {
            styleClass.add("match-words")
            tooltip = Tooltip("Match Words (ctrl+W)")
            selectedProperty().bindBidirectional(matcher.matchWordsProperty)
        }

        with(status) {
            styleClass.add("status")
            textProperty().bind(matcher.statusProperty)
        }
    }

    /**
     * This is a work-around for a bug in ComboBox.
     * I want to focus on [find], but doing find.requestFocus causes the caret to be hidden.
     * See https://stackoverflow.com/questions/40239400/javafx-8-missing-caret-in-switch-editable-combobox
     */
    fun requestFocus() {
        find.onSceneAvailable {
            find.requestFocusWithCaret()
            // Without a run later, the selection is ignored (or is de-selected), when pressing the "search" button.
            Platform.runLater {
                find.editor.selectAll()
            }
        }
    }

    /**
     * Up/down keys move through the matches. Enter and shift+Enter move down/up through the searches,
     * looping if you get to the end/start of the list.
     *
     * I've added mnemonics to the checkboxes, however, on Linux using JavaFX 8, using the mnemonic
     * only moves the focus to that checkbox, it doesn't actually change its state. Grr. So I've
     * created ANOTHER shortcut, (ctrl+ instead of alt+) which actually changes the state!
     */
    fun keyPressed(event: KeyEvent) {
        var consume = true
        if (event.isShortcutDown) {

            when (event.code) {
                KeyCode.E -> matcher.matchRegex = !matcher.matchRegex
                KeyCode.M -> matcher.matchCase = !matcher.matchCase
                KeyCode.W -> matcher.matchWords = !matcher.matchWords
                else -> {
                    consume = false
                }
            }

        } else {
            when (event.code) {
                KeyCode.UP -> matcher.previousMatch()
                KeyCode.DOWN -> matcher.nextMatch()
                KeyCode.ENTER -> {
                    if (event.isShiftDown) {
                        matcher.previousMatch(true)
                    } else {
                        matcher.nextMatch(true)
                    }
                }
                else -> {
                    consume = false
                }
            }
        }

        if (consume) {
            event.consume()
        }
    }

    fun createToggleButton(): ToggleButton {
        val button = ToggleButton()
        with(button) {
            tooltip = Tooltip("Find")
            loadGraphic(FindBar::class.java, "find.png")
            selectedProperty().bindBidirectional(toolBar.visibleProperty())
        }

        return button
    }


    companion object {
        @JvmStatic
        val findHistory = ObservableListWrapper(mutableListOf<String>())
    }
}
