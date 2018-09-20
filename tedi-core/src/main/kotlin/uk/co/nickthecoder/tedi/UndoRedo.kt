package uk.co.nickthecoder.tedi

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty

abstract class UndoRedo {

    val undoableProperty = SimpleBooleanProperty(this, "undoable", false)
    var undoable
        get() = undoableProperty.get()
        set(v) {
            undoableProperty.set(v)
        }

    val redoableProperty = SimpleBooleanProperty(this, "redoable", false)
    var redoable
        get() = redoableProperty.get()
        set(v) {
            redoableProperty.set(v)
        }

    open fun beginCompound() {}
    open fun endCompound() {}

    abstract fun undo()

    abstract fun redo()

    abstract fun replaceText(start: Int, end: Int, text: String)

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
     * The index of the change that will be applied if undo() is called.
     * The redo will be index + 1
     */
    private var index = -1
    private val changes = mutableListOf<Change>()

    private var compoundChange: CompoundChange? = null

    init {
        destroyStandardUndoList()
    }

    fun add(change: Change) {
        val cc = compoundChange
        if (cc == null) {
            while (changes.size > index + 1) {
                changes.removeAt(changes.size - 1)
            }
            changes.add(change)
            index++
            updateState()
        } else {
            cc.changes.add(change)
        }
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
    }

    inner class ReplaceTextChange(val start: Int, oldEnd: Int, val newText: String) : Change {

        val oldText: String

        init {
            inUndoRedo = true
            tediArea.selectRange(start, oldEnd)
            oldText = tediArea.selectedText
            inUndoRedo = false
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
