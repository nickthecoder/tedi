package uk.co.nickthecoder.tedi.ui

import com.sun.javafx.collections.ObservableListWrapper
import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import uk.co.nickthecoder.tedi.loadGraphic
import uk.co.nickthecoder.tedi.onSceneAvailable
import uk.co.nickthecoder.tedi.requestFocusWithCaret

/**
 * An example GUI for use with [TextInputControlMatcher].
 *
 * Add [toolBar] to your scene.
 * Making the [toolBar] hidden (toolbar.isVisible = false) will disable the [TextInputControlMatcher].
 *
 * If your application has multiple TediAreas (for example inside a TabPane), then you can either :
 * 1. Create a single matcher, and a single [FindBar] for uses
 * 2. Create a matcher and a [FindBar] for each TediArea.
 *
 * If doing the former, then you need to set the TextInputControlMatcher.textInputControl appropriately
 * (for example, whenever you select a new tab, or whenever a TediArea gains focus).
 */
open class FindBar(val matcher: TextInputControlMatcher) {

    val toolBar = ToolBar()

    val find = HistoryComboBox(findHistory)

    val prev = Button()

    val next = Button()

    val matchCase = CheckBox("Match Case")

    val matchRegex = CheckBox("Regex")

    val matchWords = CheckBox("Words")

    val status = Label()

    init {

        find.isEditable = true

        toolBar.visibleProperty().addListener { _, _, newValue ->
            if (newValue == true) {
                requestFocus()
            }
        }

        matcher.inUseProperty.bindBidirectional(toolBar.visibleProperty())

        toolBar.setVisible(true)

        with(toolBar) {
            styleClass.add("tedi-find-bar")
            items.addAll(find, prev, next, Separator(), matchCase, matchRegex, matchWords, Separator(), status)
        }

        with(find) {
            promptText = "find"
            styleClass.add("find")
            valueProperty().bindBidirectional(matcher.findProperty)
            addEventHandler(KeyEvent.KEY_PRESSED) { keyPressedInFindField(it) }
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
        }
    }

    fun keyPressedInFindField(event: KeyEvent) {
        var consume = true
        if (event.isControlDown) {

            when (event.code) {
                KeyCode.E -> matcher.matchRegex = !matcher.matchRegex
                KeyCode.M -> matcher.matchCase = !matcher.matchCase
                KeyCode.W -> matcher.matchWords = !matcher.matchWords
                else -> consume = false
            }

        } else {
            when (event.code) {
                KeyCode.ENTER -> matcher.startFind()
                KeyCode.UP -> matcher.previousMatch()
                KeyCode.DOWN -> matcher.nextMatch()
                else -> consume = false
            }
        }

        if (consume) event.consume()
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
        val findHistory = ObservableListWrapper(mutableListOf<String>())
    }
}
