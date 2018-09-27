package uk.co.nickthecoder.tedi.ui

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.scene.control.IndexRange
import javafx.scene.control.TextInputControl
import uk.co.nickthecoder.tedi.FillStyleHighlight
import uk.co.nickthecoder.tedi.Highlight
import uk.co.nickthecoder.tedi.HighlightRange
import java.util.regex.Pattern

/**
 * Performs find and replace operations on a TextInputControl.
 * The [control] can be a TextField, a TextArea or a TediArea.
 */
abstract class AbstractMatcher<C : TextInputControl>(control: C) {

    val controlProperty = SimpleObjectProperty<C>(control)
    var control: C
        get() = controlProperty.get()
        set(v) {
            controlProperty.set(v)
        }

    val findProperty = SimpleStringProperty("")
    var find: String?
        get() = findProperty.get()
        set(v) {
            findProperty.set(v)
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

    val matchWordsProperty = SimpleBooleanProperty(false)
    var matchWords: Boolean
        get() = matchWordsProperty.get()
        set(v) {
            matchWordsProperty.set(v)
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

    protected val controlChanged
            = ChangeListener<C> { _, oldValue, newValue -> controlChanged(oldValue, newValue) }

    protected val selectionChangedListener = ChangeListener<IndexRange> { _, _, _ ->
        selectionChanged()
    }

    /**
     * When the find bar is hidden, set this to false.
     * When [inUse] == true, then matches will be restarted whenever the text changes, so
     * it will be needless inefficient to keep inUse = true for longer than needed.
     */
    val inUseProperty = SimpleBooleanProperty(true)
    var inUse: Boolean
        get() = inUseProperty.get()
        set(v) {
            inUseProperty.set(v)
            if (!v) {
                clearMatches()
            }
        }

    /**
     * Determines how matched results will look (Only supported when used with a TediAreaMatcher).
     */
    var matchHighlight: Highlight = FillStyleHighlight("-fx-fill: black;", "-fx-fill: yellow;")

    /**
     * Determines how replacements will look (Only supported when used with a TediAreaMatcher).
     */
    var replacementHighlight: Highlight = FillStyleHighlight("-fx-fill: black;", "-fx-fill: #ccffcc;")


    protected var pattern = Pattern.compile("")

    protected var matcher = pattern.matcher("")

    /**
     * Parts of the document that match
     */
    protected val matches = mutableListOf<HighlightRange>()

    /**
     * Parts of the document which have been REPLACED, i.e. they no longer match, but may still be of
     * interest. TediAreaMatcher highlights replacements in a different colour.
     */
    protected val replacements = mutableListOf<HighlightRange>()

    protected var currentMatchIndex = -1

    init {
        controlProperty.addListener { _, oldValue, newValue -> controlChanged(oldValue, newValue) }
        findProperty.addListener { _, _, _ -> startFind() }
        matchCaseProperty.addListener { _, _, _ -> startFind() }
        matchRegexProperty.addListener { _, _, _ -> startFind() }
        matchWordsProperty.addListener { _, _, _ -> startFind() }
    }

    open fun controlChanged(oldValue: C?, newValue: C?) {
        oldValue?.selectionProperty()?.removeListener(selectionChangedListener)
        newValue?.selectionProperty()?.addListener(selectionChangedListener)
        startFind(false)
    }

    open fun selectionChanged() {
        val selection = control.selection
        currentMatchIndex = findMatchIndex(selection.start, selection.end)
        matchSelected = currentMatchIndex >= 0
        updatePrevNext()
    }

    open fun startFind(changeSelection: Boolean = true) {
        clearMatches()
        currentMatchIndex = -1

        if (inUse && find?.isNotEmpty() ?: false) {

            val caseFlag = if (matchCase) 0 else Pattern.CASE_INSENSITIVE
            val literalFlag = if (matchRegex) 0 else Pattern.LITERAL
            if (matchWords) {
                if (matchRegex) {
                    pattern = Pattern.compile("\\b${find}\\b", caseFlag + literalFlag)
                } else {
                    pattern = Pattern.compile("\\b${Pattern.quote(find)}\\b", caseFlag)
                }
            } else {
                pattern = Pattern.compile(find, caseFlag + literalFlag + Pattern.MULTILINE)
            }
            matcher = pattern.matcher(control.text)
            val caret = control.caretPosition
            while (matcher.find()) {
                addMatch(matcher.start(), matcher.end())
                if (matcher.start() >= caret && currentMatchIndex < 0) {
                    currentMatchIndex = matches.size - 1
                }
            }
            // If no matches were found AFTER the caret position, use the FIRST match (if there is one).
            if (currentMatchIndex < 0) {
                currentMatchIndex = 0
            }
            if (changeSelection) {
                highlightCurrentMatch()
            }
        }
        updatePrevNext()
    }

    open protected fun clearMatches() {
        matches.clear()
        replacements.clear()
        updatePrevNext()
    }

    open protected fun addMatch(start: Int, end: Int): HighlightRange {
        val match = HighlightRange(start, end, matchHighlight)
        matches.add(match)
        return match
    }

    open protected fun removeMatch(index: Int): HighlightRange {
        return matches.removeAt(index)
    }

    open protected fun addReplacement(start: Int, end: Int): HighlightRange {
        val hr = HighlightRange(start, end, replacementHighlight)
        replacements.add(hr)
        return hr
    }

    open protected fun updatePrevNext() {
        if (currentMatchIndex < 0) {
            hasPrev = matches.isNotEmpty() && control.caretPosition > matches[0].start
            hasNext = matches.isNotEmpty() && control.caretPosition < matches.last().start
        } else {
            hasPrev = currentMatchIndex > 0
            hasNext = currentMatchIndex < matches.size - 1
        }

        status = if (find?.isEmpty() ?: true) {
            ""
        } else if (inUse) {
            if (matches.isEmpty()) {
                "No matches"
            } else if (matches.size == 1) {
                "One match"
            } else {
                if (currentMatchIndex >= 0) {
                    "${currentMatchIndex + 1} of ${matches.size} matches"
                } else {
                    "${matches.size} matches"
                }
            }
        } else {
            "idle"
        }
    }

    open fun previousMatch(loop: Boolean = false) {
        // If we aren't AT one of the matches, then find the previous one based on the caret's position.
        if (currentMatchIndex < 0) {
            val caret = control.caretPosition
            for (i in 0..matches.size - 1) {
                val match = matches[i]
                if (match.start >= caret) {
                    break
                }
                currentMatchIndex = i
            }
        } else {
            if (currentMatchIndex > 0) {
                currentMatchIndex--
            } else if (loop && matches.size > 1) {
                currentMatchIndex = matches.size - 1
            }
        }
        highlightCurrentMatch()
        updatePrevNext()
    }

    /**
     * @param [loop] Go to the first match if we've reached the bottom
     */
    open fun nextMatch(loop: Boolean = false) {
        // If we aren't AT one of the matches, then find the next one based on the caret's position.
        if (currentMatchIndex < 0) {
            val caret = control.caretPosition
            for (i in 0..matches.size - 1) {
                val match = matches[i]
                if (match.start >= caret) {
                    currentMatchIndex = i
                    break
                }
            }
        } else {
            if (currentMatchIndex < matches.size - 1) {
                currentMatchIndex++
            } else if (loop && matches.size > 1) {
                currentMatchIndex = 0
            }
        }
        highlightCurrentMatch()
        updatePrevNext()
    }

    /**
     * Highlight the current match. This changes the control's selection.
     */
    open protected fun highlightCurrentMatch() {
        if (currentMatchIndex >= 0 && currentMatchIndex < matches.size) {
            val match = matches[currentMatchIndex]
            control.selectRange(match.end, match.start)
        }
    }

    open fun replace(replacement: String) {
        if (currentMatchIndex >= 0) {
            val match = matches[currentMatchIndex]
            removeMatch(currentMatchIndex)
            addReplacement(match.start, match.end)

            control.selectionProperty().removeListener(selectionChangedListener)
            control.replaceText(match.start, match.end, replacement)
            control.selectionProperty().addListener(selectionChangedListener)

            highlightCurrentMatch()
            updatePrevNext()
            if (currentMatchIndex >= matches.size) {
                currentMatchIndex = -1
            }
        }
    }

    open fun replaceAll(replacement: String) {
        currentMatchIndex = matches.size - 1
        while (currentMatchIndex >= 0) {
            val match = matches[currentMatchIndex]
            control.replaceText(match.start, match.end, replacement)
            currentMatchIndex--
        }
        replacements.addAll(matches)
        matches.clear()
        currentMatchIndex = -1
        updatePrevNext()
    }

    open fun findMatchIndex(start: Int, end: Int): Int {
        // Optimisation : Test the current match first, as it is the most likely.
        if (currentMatchIndex >= 0 && currentMatchIndex < matches.size) {
            if (matches[currentMatchIndex].start == start && matches[currentMatchIndex].end == end) {
                return currentMatchIndex
            }
        }

        matches.forEachIndexed { i, m ->
            if (m.start == start && m.end == end) return i
            if (m.start > start) return -1 // Matches are in order, so we can end early
        }
        return -1
    }
}
