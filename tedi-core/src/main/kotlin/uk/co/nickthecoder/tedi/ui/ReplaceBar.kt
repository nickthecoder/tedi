package uk.co.nickthecoder.tedi.ui

import com.sun.javafx.collections.ObservableListWrapper
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToolBar
import javafx.scene.control.Tooltip
import uk.co.nickthecoder.tedi.loadGraphic
import uk.co.nickthecoder.tedi.onSceneAvailable
import uk.co.nickthecoder.tedi.requestFocusWithCaret

open class ReplaceBar(val matcher: TextInputControlMatcher) {

    val toolBar = ToolBar()

    val replacement = HistoryComboBox(replacementHistory)

    val replace = Button("Replace")

    val replaceAll = Button("Replace All")

    init {

        with(toolBar) {
            styleClass.add("tedi-replace")
            items.addAll(replacement, replace, replaceAll)
        }

        with(replacement) {
            promptText = "replacement"
            styleClass.add("replacement")
        }

        with(replace) {
            styleClass.add("replace")
            disableProperty().bind(matcher.matchSelectedProperty.not())
            onAction = EventHandler { matcher.replace(replacement.value ?: "") }
        }

        with(replaceAll) {
            styleClass.add("replaceAll")
            disableProperty().bind(matcher.matchSelectedProperty.not())
            onAction = EventHandler { matcher.replaceAll(replacement.value ?: "") }
        }

        matcher.inUseProperty.addListener { _, _, newValue ->
            if (!newValue) {
                toolBar.isVisible = false
            }
        }

        toolBar.visibleProperty().addListener { _, _, newValue ->
            if (newValue) {
                matcher.inUse = true
            }
        }
    }

    /**
     * This is a work-around for a bug in ComboBox.
     * I want to focus on [search], but doing search.requestFocus causes the caret to be hidden.
     * See https://stackoverflow.com/questions/40239400/javafx-8-missing-caret-in-switch-editable-combobox
     */
    fun requestFocus() {
        replacement.onSceneAvailable {
            replacement.requestFocusWithCaret()
        }
    }

    fun createToggleButton(): ToggleButton {
        val button = ToggleButton()
        with(button) {
            loadGraphic(FindBar::class.java, "replace.png")
            selectedProperty().bindBidirectional(toolBar.visibleProperty())
            tooltip = Tooltip("Find and Replace")
        }
        return button
    }

    companion object {
        val replacementHistory = ObservableListWrapper(mutableListOf<String>())
    }
}