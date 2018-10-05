package uk.co.nickthecoder.tedi

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import uk.co.nickthecoder.tedi.util.getPrivateField
import uk.co.nickthecoder.tedi.util.setPrivateField
import java.util.*

abstract class UndoRedo {

    protected val undoableProperty = SimpleBooleanProperty(this, "undoable", false)
    fun undoableProperty() = undoableProperty
    var undoable
        get() = undoableProperty.get()
        set(v) {
            undoableProperty.set(v)
        }

    protected val redoableProperty = SimpleBooleanProperty(this, "redoable", false)
    fun redoableProperty() = redoableProperty
    var redoable
        get() = redoableProperty.get()
        set(v) {
            redoableProperty.set(v)
        }

    open fun beginCompound() {}
    open fun endCompound() {}

    abstract fun clear()

    abstract fun undo()

    abstract fun redo()

    abstract fun replaceText(start: Int, end: Int, text: String)

    /**
     * Called from TediArea after each action with affects TextInputControl's default undo/redo list
     * (replaceText and selectRange).
     * When we are using BetterUndoRedo, this will clear TextInputControl's undo/redo list.
     */
    abstract fun postChange()
}

/**
 * Uses the built-in Undo/Redo feature of TextInputControl
 */
class StandardUndoRedo(val tediArea: TediArea) : UndoRedo() {

    init {
        undoableProperty.bind(tediArea.undoableProperty())
        redoableProperty.bind(tediArea.redoableProperty())
    }

    override fun replaceText(start: Int, end: Int, text: String) {}

    override fun postChange() {}

    override fun undo() {
        tediArea.undo()
    }

    override fun redo() {
        tediArea.redo()
    }

    override fun clear() {
        TODO("not implemented")
    }
}

/**
 * Improves on the standard undo/redo features supplied by TextInputControl.
 *
 * Unlike TextInputControl, a simple caret movement is NOT considered a change
 * to be recorded in the undo/redo list.
 *
 * It allows for "compound" actions, such as "Replace All" to be atomic.
 * i.e. Hitting "undo" will undo ALL of the replace operations.
 * In contrast, TextInputControl make a separate change for each replacements,
 * so if you wanted to undo a "Replace All", you would have to hit "Undo"
 * many times! Yuck!
 */
class BetterUndoRedo(val tediArea: TediArea) : UndoRedo() {

    private var inUndoRedo: Boolean = false

    /**
     * Used in conjunction with [mergeThreshold] to decide if new changes should be merged with old ones.
     */
    private var previousAddTime = 0L

    /**
     * Number of milliseconds between changes, before they can be merged.
     * This means a pause in typing will NOT be merged with previous changes.
     * The default is half a second.
     */
    val mergeThreshold = 500

    /**
     * The index of the change that will be applied if undo() is called.
     * The redo will be index + 1
     */
    private var index = -1
    private val changes = mutableListOf<Change>()

    private var compoundChange: CompoundChange? = null

    init {
        destroyStandardUndoList()
    }

    override fun clear() {
        index = -1
        changes.clear()
        updateState()
    }

    fun add(change: Change) {
        val cc = compoundChange
        if (cc == null) {
            while (changes.size > index + 1) {
                changes.removeAt(changes.size - 1)
            }

            // Can we merge this with the top-most change?
            val now = Date().time
            if (previousAddTime + mergeThreshold > now && changes.isNotEmpty() && change.mergeWith(changes.last())) {
                // Do nothing, the merge is sufficient
            } else {
                changes.add(change)
                index++
                updateState()
            }

        } else {
            cc.changes.add(change)
        }
        previousAddTime = Date().time
    }

    override fun beginCompound() {
        if (compoundChange != null) {
            throw IllegalStateException("A CompoundChange is already in use")
        }
        compoundChange = CompoundChange()
    }

    override fun endCompound() {
        val cc = compoundChange
        cc ?: throw IllegalStateException("A CompoundChange is not in use")

        compoundChange = null
        if (cc.changes.isNotEmpty()) {
            // Only add the compound if it contains child changes.
            add(cc)
        }
    }

    override fun postChange() {
        destroyStandardUndoList()
    }

    override fun replaceText(start: Int, end: Int, text: String) {
        if (!inUndoRedo) {
            add(ReplaceTextChange(start, end, text))
            updateState()
        }
    }

    fun updateState() {
        destroyStandardUndoList()
        undoable = (index >= 0)
        redoable = (index < changes.size - 1)
        // println("Updated state undoable=$undoable redoable=$redoable index=$index Changes=$changes")
    }

    override fun undo() {
        if (undoable) {
            inUndoRedo = true
            changes[index].applyUndo()
            index--
            inUndoRedo = false
            updateState()
        }
    }

    override fun redo() {
        if (redoable) {
            inUndoRedo = true
            changes[index + 1].applyRedo()
            index++
            inUndoRedo = false
            updateState()
        }
    }

    /**
     * Only issue undo warnings once.
     */
    private var undoWarningIssued = false

    /**
     * We can't have two undo/redo lists in operation at the same time!
     * So, let's try to scupper TextInputControl's undo/redo list. Alas, this requires access to private fields,
     * so I've done my best to fail gracefully if the private fields change in a later version of JavaFX.
     * A warning will be issued once.
     * Even if this fails, nothing bad will happen as long as only one of the undo/redo lists are used.
     */
    private fun destroyStandardUndoList() {
        try {
            val head = tediArea.getPrivateField("undoChangeHead")
            head?.setPrivateField("next", null)
            tediArea.setPrivateField("undoChange", head)

            (tediArea.getPrivateField("undoable") as BooleanProperty).set(false)
            (tediArea.getPrivateField("redoable") as BooleanProperty).set(false)
        } catch (e: Exception) {
            if (!undoWarningIssued) {
                undoWarningIssued = true
                println("WARNING : Failed to destroy the existing undo/redo feature of TextInputControl")
                println("          Calls to TextInputControl.undo() and redo() and the properties undoable and redoable")
                println("          may cause weird behaviour. (You shouldn't be using these anyway!)")
            }
        }
    }

    interface Change {
        fun applyUndo()
        fun applyRedo()
        /**
         * Return true, if this change can be merged with [other], in which case, this
         * change will NOT be added to the list, and instead, the merged one will suffice.
         */
        fun mergeWith(other: Change): Boolean = false
    }

    inner class ReplaceTextChange(var start: Int, oldEnd: Int, var newText: String) : Change {

        var oldText: String

        init {
            inUndoRedo = true
            tediArea.selectRange(start, oldEnd)
            oldText = tediArea.selectedText
            inUndoRedo = false
        }

        override fun mergeWith(other: Change): Boolean {
            if (other is ReplaceTextChange) {

                if (start == other.start + other.newText.length && other.oldText.isEmpty()) {
                    // Additions to the end of the previous insertion can be merged
                    other.newText = other.newText + newText
                    return true
                } else if (start == other.start && other.newText.isEmpty() && newText.isEmpty()) {
                    // Deletions from the start of the previous insertion can be merged
                    other.oldText = other.oldText + oldText
                    return true
                } else if (start == other.start - oldText.length && newText.isEmpty()) {
                    other.start = start
                    other.oldText = oldText + other.oldText
                    return true
                } else {
                    return false
                }
            } else {
                return false
            }
        }

        override fun applyUndo() {
            tediArea.replaceText(start, start + newText.length, oldText)
        }

        override fun applyRedo() {
            tediArea.replaceText(start, start + oldText.length, newText)
        }

        override fun toString() = "ReplaceTextChange start:$start was:$oldText now:$newText"
    }

    /**
     * Places all changes into a group.
     * For example, when doing a "Replace All", each of the individual changes is placed into a CompoundChange,
     * so that undo/redo will apply all these changes together.
     */
    class CompoundChange : Change {

        val changes = mutableListOf<Change>()

        override fun applyRedo() {
            changes.forEach { it.applyRedo() }
        }

        override fun applyUndo() {
            changes.asReversed().forEach { it.applyUndo() }
        }

    }

}
