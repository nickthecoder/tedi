package uk.co.nickthecoder.tedi.ui

import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.ToolBar

open class ReplaceBar(val matcher: TextInputControlMatcher) {

    val toolBar = ToolBar()

    val replacement = TextField()

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
            onAction = EventHandler { matcher.replace(replacement.text) }
        }

        with(replaceAll) {
            styleClass.add("replaceAll")
            disableProperty().bind(matcher.matchSelectedProperty.not())
            onAction = EventHandler { matcher.replaceAll(replacement.text) }
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

}