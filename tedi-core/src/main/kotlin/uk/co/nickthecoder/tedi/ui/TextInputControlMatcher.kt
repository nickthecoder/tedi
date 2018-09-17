package uk.co.nickthecoder.tedi.ui

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.scene.control.TextInputControl
import uk.co.nickthecoder.tedi.TediArea
import java.util.regex.Pattern

/**
 * Performs search and replace operations on a TextInputControl.
 * The [textInputControl] can be a TextField, a TextArea or a TediArea.
 *
 * When highlighting of matches is implemented in TediArea, then a sub-class or TextInputControl will be created
 * to use that feature.
 */
open class TextInputControlMatcher(tediArea: TediArea) {

    val textInputControlProperty = SimpleObjectProperty<TextInputControl>(tediArea)
    var textInputControl: TextInputControl
        get() = textInputControlProperty.get()
        set(v) {
            textInputControlProperty.set(v)
        }

    val searchProperty = SimpleStringProperty("")
    var search: String
        get() = searchProperty.get()
        set(v) {
            searchProperty.set(v)
        }

    val matchCaseProperty = SimpleBooleanProperty(false)
    var matchCase: Boolean
        get() = matchCaseProperty.get()
        set(v) {
            matchCaseProperty.set(v)
        }

    val matchRegexProperty = SimpleBooleanProperty(false)
    var matchRegex: Boolean
        get() = matchRegexProperty.get()
        set(v) {
            matchRegexProperty.set(v)
        }

    val statusProperty = SimpleStringProperty("")
    var status: String
        get() = statusProperty.get()
        set(v) {
            statusProperty.set(v)
        }

    val hasNextProperty = SimpleBooleanProperty(false)
    var hasNext: Boolean
        get() = hasNextProperty.get()
        set(v) {
            hasNextProperty.set(v)
        }


    val hasPrevProperty = SimpleBooleanProperty(false)
    var hasPrev: Boolean
        get() = hasNextProperty.get()
        set(v) {
            hasPrevProperty.set(v)
        }

    val matchSelectedProperty = SimpleBooleanProperty(false)
    var matchSelected: Boolean
        get() = matchSelectedProperty.get()
        set(v) {
            matchSelectedProperty.set(v)
        }

    /**
     * When the search bar is hidden, set this to false.
     * When [inUse] == true, then searches will be restarted whenever the text changes, so
     * it will be needless inefficient to keep inUse = true for longer than needed.
     */
    val inUseProperty = SimpleBooleanProperty(true)
    var inUse: Boolean
        get() = inUseProperty.get()
        set(v) {
            inUseProperty.set(v)
        }

    protected var pattern = Pattern.compile("")

    protected var matcher = pattern.matcher("")

    protected var textChangedListener = ChangeListener<String> { _, _, _ -> textChanged() }

    protected var selectionChangedListener = ChangeListener<Number> { _, _, _ -> selectionChanged() }

    protected val matches = mutableListOf<Match>()

    protected var currentMatchIndex = -1

    protected var performingReplace = false

    init {
        textInputControlProperty.addListener { _, oldValue, newValue -> textInputControlChanged(oldValue, newValue) }
        searchProperty.addListener { _, _, _ -> startSearch() }
        matchCaseProperty.addListener { _, _, _ -> startSearch() }
        matchRegexProperty.addListener { _, _, _ -> startSearch() }
        inUseProperty.addListener { _, _, _ -> startSearch() }

        tediArea.caretPositionProperty().addListener(selectionChangedListener)
        tediArea.anchorProperty().addListener(selectionChangedListener)
        tediArea.textProperty().addListener(textChangedListener)
    }

    protected fun textInputControlChanged(oldValue: TextInputControl, newValue: TextInputControl) {
        with(oldValue) {
            textProperty().removeListener(textChangedListener)
            caretPositionProperty().removeListener(selectionChangedListener)
            anchorProperty().removeListener(selectionChangedListener)
        }

        with(newValue) {
            newValue.textProperty().addListener(textChangedListener)
            caretPositionProperty().addListener(selectionChangedListener)
            anchorProperty().addListener(selectionChangedListener)
        }
    }

    open fun textChanged() {
        if (performingReplace) {
            startSearch()
            performingReplace = false
        }
    }

    open fun startSearch(changeSelection: Boolean = true) {
        matches.clear()
        currentMatchIndex = -1

        if (inUse && search.isNotEmpty()) {

            val flags = (if (matchCase) 0 else Pattern.CASE_INSENSITIVE) + if (matchRegex) 0 else Pattern.LITERAL
            pattern = Pattern.compile(search, flags + Pattern.MULTILINE)
            matcher = pattern.matcher(textInputControl.text)
            val caret = textInputControl.caretPosition
            while (matcher.find()) {
                val match = Match(matcher.start(), matcher.end())
                matches.add(match)
                if (match.start >= caret && currentMatchIndex < 0) {
                    currentMatchIndex = matches.size - 1
                }
            }
            // If no matches were found AFTER the caret position, use the FIRST match (if there is one).
            if (currentMatchIndex < 0) {
                currentMatchIndex = 0
            }
            if (changeSelection) {
                updateSelection()
            }
        }
        updatePrevNext()
    }

    protected fun updatePrevNext() {
        hasPrev = currentMatchIndex > 0
        hasNext = currentMatchIndex < matches.size - 1
        status = if (search.isEmpty()) {
            ""
        } else if (inUse) {
            if (matches.isEmpty()) {
                "No matches"
            } else if (matches.size == 1) {
                "One match"
            } else {
                "${currentMatchIndex + 1} of ${matches.size} matches"
            }
        } else {
            "idle"
        }
    }

    open fun previousMatch() {
        if (currentMatchIndex > 0) {
            currentMatchIndex--
            updateSelection()
            updatePrevNext()
        }
    }

    open fun nextMatch() {
        if (currentMatchIndex < matches.size - 1) {
            currentMatchIndex++
            updateSelection()
            updatePrevNext()
        }
    }

    protected fun updateSelection() {
        if (currentMatchIndex >= 0 && currentMatchIndex < matches.size) {
            val match = matches[currentMatchIndex]
            textInputControl.selectRange(match.end, match.start)
        }
    }

    protected fun selectionChanged() {
        if (inUse && !performingReplace) {
            val start = Math.min(textInputControl.anchor, textInputControl.caretPosition)
            val end = Math.max(textInputControl.anchor, textInputControl.caretPosition)
            currentMatchIndex = findMatchIndex(Match(start, end))
            matchSelected = currentMatchIndex >= 0
        } else {
            matchSelected = false
        }
    }

    open fun replace(replacement: String) {
        if (matchSelected) {
            performingReplace = true
            textInputControl.replaceSelection(replacement)
        }
    }

    open fun replaceAll(replacement: String) {
        currentMatchIndex = matches.size - 1
        while (currentMatchIndex >= 0) {
            updateSelection()
            replace(replacement)
            currentMatchIndex--
        }
    }

    open fun findMatchIndex(match: Match): Int {
        // Optimisation : Test the current match first, as it is the most likely.
        if (currentMatchIndex >= 0 && currentMatchIndex < matches.size) {
            if (matches[currentMatchIndex] == match) {
                return currentMatchIndex
            }
        }

        matches.forEachIndexed { i, m ->
            if (match == m) return i
            if (m.start > match.start) return -1 // Matches are in order, so we can end early
        }
        return -1
    }

    data class Match(val start: Int, val end: Int)
}
