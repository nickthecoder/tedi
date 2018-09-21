package uk.co.nickthecoder.tedi.ui

import com.sun.javafx.collections.ObservableListWrapper
import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import uk.co.nickthecoder.tedi.loadGraphic
import uk.co.nickthecoder.tedi.onSceneAvailable
import uk.co.nickthecoder.tedi.requestFocusOnSceneAvailable
import uk.co.nickthecoder.tedi.requestFocusWithCaret

/**
 * An example GUI for use with [TextInputControlMatcher].
 *
 * Add [toolBar] to your scene.
 * Making the [toolBar] hidden (toolbar.isVisible = false) will disable the [TextInputControlMatcher].
 *
 * If your application has multiple TediAreas (for example inside a TabPane), then you can either :
 * 1. Create a single matcher, and a single SearchBar for uses
 * 2. Create a matcher and a SearchBar for each TediArea.
 *
 * If doing the former, then you need to set the TextInputControlMatcher.textInputControl appropriately
 * (for example, whenever you select a new tab, or whenever a TediArea gains focus).
 */
open class SearchBar(val matcher: TextInputControlMatcher) {

    val toolBar = ToolBar()

    val search = HistoryComboBox(searchHistory)

    val prev = Button()

    val next = Button()

    val matchCase = CheckBox("Match Case")

    val matchRegex = CheckBox("Regex")

    val matchWords = CheckBox("Words")

    val status = Label()

    init {

        search.isEditable = true

        toolBar.visibleProperty().addListener { _, _, newValue ->
            if (newValue == true) {
                search.requestFocusOnSceneAvailable()
            }
        }

        matcher.inUseProperty.bindBidirectional(toolBar.visibleProperty())

        toolBar.setVisible(true)

        with(toolBar) {
            styleClass.add("tedi-search-bar")
            items.addAll(search, prev, next, Separator(), matchCase, matchRegex, matchWords, Separator(), status)
        }

        with(search) {
            promptText = "search"
            styleClass.add("search")
            valueProperty().bindBidirectional(matcher.searchProperty)
            addEventHandler(KeyEvent.KEY_PRESSED) { keyPressedInSearchField(it) }
        }

        with(prev) {
            styleClass.add("prev")
            disableProperty().bind(matcher.hasPrevProperty.not())
            loadGraphic(SearchBar::class.java, "prev.png")
            tooltip = Tooltip("Previous match (Up)")
            onAction = EventHandler { matcher.previousMatch() }
        }

        with(next) {
            styleClass.add("next")
            disableProperty().bind(matcher.hasNextProperty.not())
            loadGraphic(SearchBar::class.java, "next.png")
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
     * I want to focus on [search], but doing search.requestFocus causes the caret to be hidden.
     * See https://stackoverflow.com/questions/40239400/javafx-8-missing-caret-in-switch-editable-combobox
     */
    fun requestFocus() {
        search.onSceneAvailable {
            search.requestFocusWithCaret()
        }
    }

    fun keyPressedInSearchField(event: KeyEvent) {
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
                KeyCode.ENTER -> matcher.startSearch()
                KeyCode.UP -> matcher.previousMatch()
                KeyCode.DOWN -> matcher.nextMatch()
                else -> consume = false
            }
        }

        if (consume) event.consume()
    }

    companion object {
        val searchHistory = ObservableListWrapper(mutableListOf<String>())
    }
}
