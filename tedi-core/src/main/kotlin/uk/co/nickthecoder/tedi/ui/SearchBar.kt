package uk.co.nickthecoder.tedi.ui

import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import uk.co.nickthecoder.tedi.loadGraphic
import uk.co.nickthecoder.tedi.requestFocusOnSceneAvailable

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

    val search = TextField()

    val prev = Button()

    val next = Button()

    val matchCase = CheckBox("Match Case")

    val matchRegex = CheckBox("Regex")

    val status = Label()

    init {

        toolBar.visibleProperty().addListener { _, _, newValue ->
            if (newValue == true) {
                search.requestFocusOnSceneAvailable()
            }
        }

        matcher.inUseProperty.bindBidirectional(toolBar.visibleProperty())

        toolBar.setVisible(true)

        with(toolBar) {
            styleClass.add("tedi-search-bar")
            items.addAll(search, prev, next, Separator(), matchCase, matchRegex, Separator(), status)
        }

        with(search) {
            promptText = "search"
            styleClass.add("search")
            textProperty().bindBidirectional(matcher.searchProperty)
            addEventFilter(KeyEvent.KEY_PRESSED) { keyPressedInSearchField(it) }
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

        with(status) {
            styleClass.add("status")
            textProperty().bind(matcher.statusProperty)
        }
    }

    fun keyPressedInSearchField(event: KeyEvent) {
        var consume = true
        if (event.isControlDown) {

            when (event.code) {
                KeyCode.E -> matcher.matchRegex = !matcher.matchRegex
                KeyCode.M -> matcher.matchCase = !matcher.matchCase
                KeyCode.DOWN -> matcher.nextMatch()
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

}
