package uk.co.nickthecoder.tedi.ui

import com.sun.javafx.collections.ObservableListWrapper
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToolBar
import javafx.scene.control.Tooltip
import uk.co.nickthecoder.tedi.loadGraphic
import uk.co.nickthecoder.tedi.onSceneAvailable
import uk.co.nickthecoder.tedi.requestFocusWithCaret

/**
 * An example GUI for use with [TextInputControlMatcher] or [TediAreaMatcher].
 *
 * Add [toolBar] to your scene.
 * You will also need a [FindBar] to go with this [ReplaceBar].
 */
open class ReplaceBar(val matcher: AbstractMatcher<*>) {

    val toolBar = ToolBar()

    val replacement = HistoryComboBox(replacementHistory)

    val replace = Button("_Replace")

    val replaceAll = Button("Replace _All")

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
            onAction = EventHandler { matcher.replace(replacement.editor.text ?: "") }
        }

        with(replaceAll) {
            styleClass.add("replaceAll")
            disableProperty().bind(matcher.matchSelectedProperty.not())
            onAction = EventHandler { matcher.replaceAll(replacement.editor.text ?: "") }
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
     * I want to focus on [find], but doing search.requestFocus causes the caret to be hidden.
     * See https://stackoverflow.com/questions/40239400/javafx-8-missing-caret-in-switch-editable-combobox
     */
    fun requestFocus() {
        replacement.onSceneAvailable {
            replacement.requestFocusWithCaret()
            Platform.runLater {
                replacement.editor.selectAll()
            }
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
        @JvmStatic
        val replacementHistory = ObservableListWrapper(mutableListOf<String>())
    }
}