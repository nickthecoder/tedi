package uk.co.nickthecoder.tedi.ui

import javafx.application.Platform
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.ComboBox
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import uk.co.nickthecoder.tedi.util.focusNext
import uk.co.nickthecoder.tedi.util.focusPrevious
import uk.co.nickthecoder.tedi.util.javaFXVersion
import java.util.prefs.Preferences


/**
 * A ComboBox of Strings, which remembers the values that are entered in an [ObservableList].
 * The list can be shared by more than one instance of [HistoryComboBox], and any additions to the
 * [history] will be reflected in all of them.
 *
 * The ComboBox's [items] are taken from the [history] in reverse order (so that the most recent items
 * are at the top of the list).
 *
 * New items are added at the end of the list. If the [value] of the ComboBox is set to a value already
 * in the list, then it is removed from the list, and added to the end.
 *
 * No limit is placed on the number of items in the list.
 *
 * This was created for use by [FindBar] and [ReplaceBar].
 */
class HistoryComboBox(history: ObservableList<String>)
    : ComboBox<String>() {

    private var ignoreUpdates = false

    var history: ObservableList<String> = history
        set(v) {
            field = v
            updateItems()
        }

    init {
        value = ""
        isEditable = true
        history.addListener { _: ListChangeListener.Change<out String>? -> updateItems() }
        items.addAll(history.reversed())

        valueProperty().addListener { _, _, newValue ->
            if (!ignoreUpdates && !isShowing) {
                if (newValue.isNotBlank()) {
                    history.remove(newValue)
                    history.add(newValue)
                }
            }
        }

        addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (!isShowing && event.code == KeyCode.DOWN && event.isShortcutDown) {
                event.consume()
                // Without this line, the popup gets into a weird state (up key doesn't work, and the selected item
                // is off by one). JavaFX 8
                // With this line, up key still doesn't work, but there is no selection problem
                value = ""
                show()
            }
            // If don't handle TAB and shift+TAB myself, JavaFX 8 moves the focus to the WRONG node!
            // It seems that ComboBox is REALLY buggy!
            if (javaFXVersion.startsWith("8.")) {
                if (event.code == KeyCode.TAB) {
                    if (event.isShiftDown) {
                        focusPrevious()
                    } else {
                        focusNext()
                    }
                    event.consume()
                }
            }
        }
    }

    private fun updateItems() {
        Platform.runLater {
            ignoreUpdates = true
            val old = value
            items.clear()
            items.addAll(history.reversed())
            value = old
            ignoreUpdates = false
        }
    }

    /**
     * See [saveHistory]
     */
    fun save(preferences: Preferences, maxItems: Int) {
        saveHistory(history, preferences, maxItems)
    }

    fun load(preferences: Preferences) {
        loadHistory(history, preferences)
    }


    companion object {

        /**
         * Saves the last n items of the list to [Preferences], which can then be loaded the next time
         * you application is started using [loadHistory].
         *
         * Assuming you have good package names, then [preferences] can be somethings like this :
         *
         *       Preferences.userRoot().node(MyEditorClass::class.java.name + "/search)
         *
         * or perhaps use the package name, rather than a class name :
         *
         *       Preferences.userRoot().node(MyEditorClass::class.java.package.name + "/search)
         *
         */
        @JvmStatic
        fun saveHistory(list: ObservableList<String>, preferences: Preferences, maxItems: Int) {
            preferences.keys().forEach { preferences.remove(it) }
            list.subList(Math.max(0, list.size - maxItems), list.size).forEachIndexed { i, value ->
                preferences.put(i.toString(), value)
            }
            preferences.flush()
        }

        /**
         * Use the same [preferences] as in [saveHistory].
         */
        @JvmStatic
        fun loadHistory(list: ObservableList<String>, preferences: Preferences) {
            val items = mutableListOf<String>()
            preferences.keys().map { it.toIntOrNull() ?: -1 }.sorted().forEach { key ->
                if (key >= 0) {
                    items.add(preferences.get(key.toString(), ""))
                }
            }
            list.clear()
            list.addAll(items)
        }
    }
}

