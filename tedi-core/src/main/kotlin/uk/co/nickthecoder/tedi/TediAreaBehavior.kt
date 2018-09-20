/*
 * Most of this code was copied (and convert from Java to Kotlin) from JavaFX's TextArea.
 * Therefore I have kept TextArea's copyright message. However much wasn't written by
 * Oracle, so don't blame them for my mistakes!!!
 *
 *
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package uk.co.nickthecoder.tedi

import javafx.beans.InvalidationListener
import javafx.geometry.NodeOrientation
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import uk.co.nickthecoder.tedi.javafx.BehaviorBase
import uk.co.nickthecoder.tedi.javafx.KeyBinding
import uk.co.nickthecoder.tedi.javafx.OptionalBoolean
import java.text.Bidi
import java.util.*

class TediAreaBehavior(val control: TediArea)

    : BehaviorBase<TediArea>(control, TEDI_AREA_BINDINGS) {

    private val skin: TediAreaSkin
        get() = control.skin as TediAreaSkin

    /**
     * Used to keep track of the most recent key event. This is used when
     * handling InputCharacter actions.
     */
    private var lastEvent: KeyEvent? = null

    private val textListener = InvalidationListener { _ -> invalidateBidi() }

    private var bidi: Bidi? = null
    private var rtlText: Boolean? = null

    private var shiftDown = false
    private var deferClick = false

    private var editing = false

    init {
        // Register for change events
        control.focusedProperty().addListener { _, _, _ ->
            setCaretAnimating(control.isFocused)
        }

        control.textProperty().addListener(textListener)
    }

    override fun dispose() {
        control.textProperty().removeListener(textListener)
        super.dispose()
    }

    /**************************************************************************
     * Key handling implementation                                            *
     *************************************************************************/

    /**
     * Records the last KeyEvent we saw.
     * @param e
     */
    override fun callActionForEvent(e: KeyEvent) {
        lastEvent = e
        super.callActionForEvent(e)
    }

    public override fun callAction(name: String) {

        val tediArea = getControl()

        var done = false

        if (tediArea.isEditable) {

            editing = true
            done = true
            when (name) {

                "InputCharacter" -> lastEvent?.let { defaultKeyTyped(it) }
                "Cut" -> cut()
                "Paste" -> paste()
                "DeleteFromLineStart" -> deleteFromLineStart()
                "DeletePreviousChar" -> deletePreviousChar()
                "DeleteNextChar" -> deleteNextChar()
                "DeletePreviousWord" -> deletePreviousWord()
                "DeleteNextWord" -> deleteNextWord()
                "DeleteSelection" -> deleteSelection()
                "Undo" -> tediArea.undoRedo.undo()
                "Redo" -> tediArea.undoRedo.redo()
                "InsertNewLine" -> insertNewLine()
                "TraverseOrInsertTab" -> insertTab()
                "Unindent" -> unindent()
                else -> {
                    done = false
                }
            }
            editing = false
        }

        if (!done) {
            done = true
            when (name) {
                "Copy" -> tediArea.copy()
                "SelectBackward" -> tediArea.selectBackward()
                "SelectForward" -> tediArea.selectForward()
                "SelectLeft" -> selectLeft()
                "SelectRight" -> selectRight()
                "PreviousWord" -> previousWord()
                "NextWord" -> nextWord()
                "LeftWord" -> leftWord()
                "RightWord" -> rightWord()
                "SelectPreviousWord" -> selectPreviousWord()
                "SelectNextWord" -> selectNextWord()
                "SelectLeftWord" -> selectLeftWord()
                "SelectRightWord" -> selectRightWord()
                "SelectWord" -> selectWord()
                "SelectAll" -> tediArea.selectAll()
                "Home" -> tediArea.home()
                "End" -> tediArea.end()
                "Forward" -> tediArea.forward()
                "Backward" -> tediArea.backward()
                "Right" -> nextCharacterVisually(true)
                "Left" -> nextCharacterVisually(false)
                "Fire" -> lastEvent?.let {}
                "Cancel" -> lastEvent?.let { cancelEdit(it) }
                "Unselect" -> tediArea.deselect()
                "SelectHome" -> selectHome()
                "SelectEnd" -> selectEnd()
                "SelectHomeExtend" -> selectHomeExtend()
                "SelectEndExtend" -> selectEndExtend()
                "ToParent" -> lastEvent?.let { forwardToParent(it) }

                "LineStart" -> skin.lineStart(false)
                "LineEnd" -> skin.lineEnd(false)
                "SelectLineStart" -> skin.lineStart(true)
                "SelectLineEnd" -> skin.lineEnd(true)
                "PreviousLine" -> skin.previousLine(false)
                "NextLine" -> skin.nextLine(false)
                "SelectPreviousLine" -> skin.previousLine(true)
                "SelectNextLine" -> skin.nextLine(true)
                "ParagraphStart" -> skin.paragraphStart(true, false)
                "ParagraphEnd" -> skin.paragraphEnd(true, isWindows, false)
                "SelectParagraphStart" -> skin.paragraphStart(true, true)
                "SelectParagraphEnd" -> skin.paragraphEnd(true, isWindows, true)
                "PreviousPage" -> skin.previousPage(false)
                "NextPage" -> skin.nextPage(false)
                "SelectPreviousPage" -> skin.previousPage(true)
                "SelectNextPage" -> skin.nextPage(true)
                "TraverseOrInsertTab" -> {
                    // RT-40312: Non-editabe mode means traverse instead of insert.
                    super.callAction("TraverseNext")
                    return
                }
                "Unindent" -> {
                    super.callAction("TraversePrevious")
                    return
                }
                else -> {
                    done = false
                }
            }
        }

        if (!done) {
            super.callAction(name)
        }
    }

    override fun mousePressed(e: MouseEvent?) {
        super.mousePressed(e)
        // We never respond to events if disabled
        if (!control.isDisabled) {
            if (!control.isFocused) {
                control.requestFocus()
            }

            // stop the caret animation
            setCaretAnimating(false)
            // only if there is no selection should we see the caret
            //            setCaretOpacity(if (textInputControl.dot == textInputControl.mark) then 1.0 else 0.0);

            // if the primary button was pressed
            if (e!!.button == MouseButton.PRIMARY && !(e.isMiddleButtonDown || e.isSecondaryButtonDown)) {
                val hit = skin.getHitInformation(e.x, e.y)
                val i = hit.getInsertionIndex()

                val anchor = control.anchor
                val caretPosition = control.caretPosition
                if (e.clickCount < 2 && (e.isSynthesized || anchor != caretPosition && (i > anchor && i < caretPosition || i < anchor && i > caretPosition))) {
                    // if there is a selection, then we will NOT handle the
                    // press now, but will defer until the release. If you
                    // select some text and then press down, we change the
                    // caret and wait to allow you to drag the text.
                    // When the drag concludes, then we handle the click

                    deferClick = true
                } else if (!(e.isControlDown || e.isAltDown || e.isShiftDown || e.isMetaDown || e.isShortcutDown)) {
                    when (e.clickCount) {
                        1 -> skin.positionCaret(hit, false, false)
                        2 -> mouseDoubleClick()
                        3 -> mouseTripleClick()
                    }// no-op
                } else if (e.isShiftDown && !(e.isControlDown || e.isAltDown || e.isMetaDown || e.isShortcutDown) && e.clickCount == 1) {
                    // didn't click inside the selection, so select
                    shiftDown = true
                    // if we are on mac os, then we will accumulate the
                    // selection instead of just moving the dot. This happens
                    // by figuring out past which (dot/mark) are extending the
                    // selection, and set the mark to be the other side and
                    // the dot to be the new position.
                    // everywhere else we just move the dot.
                    if (isMac) {
                        control.extendSelection(i)
                    } else {
                        skin.positionCaret(hit, true, false)
                    }
                }
            }
        }
    }

    override fun mouseDragged(e: MouseEvent?) {
        val textArea = getControl()
        // we never respond to events if disabled, but we do notify any onXXX
        // event listeners on the control
        if (!textArea.isDisabled && !e!!.isSynthesized) {
            if (e.button == MouseButton.PRIMARY && !(e.isMiddleButtonDown || e.isSecondaryButtonDown ||
                    e.isControlDown || e.isAltDown || e.isShiftDown || e.isMetaDown)) {
                skin.positionCaret(skin.getHitInformation(e.x, e.y), true, false)
            }
        }
        deferClick = false
    }

    override fun mouseReleased(e: MouseEvent?) {
        val textArea = getControl()
        super.mouseReleased(e)
        // we never respond to events if disabled, but we do notify any onXXX
        // event listeners on the control
        if (!textArea.isDisabled) {
            setCaretAnimating(false)
            if (deferClick) {
                deferClick = false
                skin.positionCaret(skin.getHitInformation(e!!.x, e.y), shiftDown, false)
                shiftDown = false
            }
            setCaretAnimating(true)
        }
    }

    private fun setCaretAnimating(play: Boolean) {
        skin.setCaretAnimating(play)
    }

    private fun mouseDoubleClick() {
        val textArea = getControl()
        textArea.previousWord()
        if (isWindows) {
            textArea.selectNextWord()
        } else {
            textArea.selectEndOfNextWord()
        }
    }

    private fun mouseTripleClick() {
        // select the line
        skin.paragraphStart(false, false)
        skin.paragraphEnd(false, isWindows, true)
    }


    /**
     * The default handler for a key typed event, which is called when none of
     * the other key bindings match. This is the method which handles basic
     * text entry.
     * @param event not null
     */
    private fun defaultKeyTyped(event: KeyEvent) {

        val textInput = getControl()
        // I'm not sure this case can actually ever happen, maybe this
        // should be an assert instead?
        if (!textInput.isEditable || textInput.isDisabled) return

        // Sometimes we get events with no key character, in which case
        // we need to bail.
        val character = event.character
        if (character.isEmpty()) return

        // Filter out control keys except control & Alt on PC or Alt on Mac
        if (event.isControlDown || event.isAltDown || isMac && event.isMetaDown) {
            if (!((event.isControlDown || isMac) && event.isAltDown)) return
        }

        // Ignore characters in the control range and the ASCII delete
        // character as well as meta key presses
        if (character[0].toInt() > 0x1F
                && character[0].toInt() != 0x7F
                && !event.isMetaDown) { // Not sure about this one
            val selection = textInput.selection
            val start = selection.start
            val end = selection.end

            replaceText(start, end, character)
        }
    }


    private fun invalidateBidi() {
        bidi = null
        rtlText = null
    }

    private fun getBidi(): Bidi {
        if (bidi == null) {
            bidi = Bidi(control.textProperty().valueSafe,
                    if (control.effectiveNodeOrientation == NodeOrientation.RIGHT_TO_LEFT)
                        Bidi.DIRECTION_RIGHT_TO_LEFT
                    else
                        Bidi.DIRECTION_LEFT_TO_RIGHT)
        }
        return bidi!!
    }

    private fun isRTLText(): Boolean {
        if (rtlText == null) {
            val bidi = getBidi()
            rtlText = bidi.isRightToLeft
        }
        return rtlText!!
    }

    private fun nextCharacterVisually(moveRight: Boolean) {
        if (moveRight != isRTLText()) {
            control.forward()
        } else {
            control.backward()
        }
    }


    private fun insertNewLine() {
        control.replaceSelection("\n")
    }

    private fun selectLeft() {
        if (isRTLText()) {
            control.selectForward()
        } else {
            control.selectBackward()
        }
    }

    private fun selectRight() {
        if (isRTLText()) {
            control.selectBackward()
        } else {
            control.selectForward()
        }
    }

    private fun deletePreviousChar() {
        deleteChar(true)
    }

    private fun deleteNextChar() {
        deleteChar(false)
    }

    private fun deletePreviousWord() {
        val textInputControl = getControl()
        val end = textInputControl.caretPosition

        if (end > 0) {
            textInputControl.previousWord()
            val start = textInputControl.caretPosition
            replaceText(start, end, "")
        }
    }

    private fun deleteNextWord() {
        val textInputControl = getControl()
        val start = textInputControl.caretPosition

        if (start < textInputControl.length) {
            nextWord()
            val end = textInputControl.caretPosition
            replaceText(start, end, "")
        }
    }

    private fun deleteSelection() {
        val textInputControl = getControl()
        val selection = textInputControl.selection

        if (selection.length > 0) {
            deleteChar(false)
        }
    }

    private fun cut() {
        val textInputControl = getControl()
        textInputControl.cut()
    }

    private fun paste() {
        val textInputControl = getControl()
        textInputControl.paste()
    }

    private fun selectPreviousWord() {
        getControl().selectPreviousWord()
    }

    private fun selectNextWord() {
        val textInputControl = getControl()
        if (isMac || isLinux) {
            textInputControl.selectEndOfNextWord()
        } else {
            textInputControl.selectNextWord()
        }
    }

    private fun selectLeftWord() {
        if (isRTLText()) {
            selectNextWord()
        } else {
            selectPreviousWord()
        }
    }

    private fun selectRightWord() {
        if (isRTLText()) {
            selectPreviousWord()
        } else {
            selectNextWord()
        }
    }

    private fun selectWord() {
        val textInputControl = getControl()
        textInputControl.previousWord()
        if (isWindows) {
            textInputControl.selectNextWord()
        } else {
            textInputControl.selectEndOfNextWord()
        }
    }

    private fun previousWord() {
        getControl().previousWord()
    }

    private fun nextWord() {
        val tediArea = getControl()
        if (isMac || isLinux) {
            tediArea.endOfNextWord()
        } else {
            tediArea.nextWord()
        }
    }

    private fun leftWord() {
        if (isRTLText()) {
            nextWord()
        } else {
            previousWord()
        }
    }

    private fun rightWord() {
        if (isRTLText()) {
            previousWord()
        } else {
            nextWord()
        }
    }

    private fun cancelEdit(event: KeyEvent) {
        forwardToParent(event)
    }

    private fun forwardToParent(event: KeyEvent) {
        if (getControl().parent != null) {
            getControl().parent.fireEvent(event)
        }
    }

    private fun selectHome() {
        getControl().selectHome()
    }

    private fun selectEnd() {
        getControl().selectEnd()
    }

    private fun selectHomeExtend() {
        getControl().extendSelection(0)
    }

    private fun selectEndExtend() {
        val textInputControl = getControl()
        textInputControl.extendSelection(textInputControl.length)
    }

    private fun extendSelectionToStartOfLine() {
        val textArea = getControl()

        val selection = textArea.selection
        val lc = textArea.lineColumnFor(selection.start)
        if (lc.second != 0) {
            textArea.selectRange(textArea.lineStartPosition(lc.first), selection.end)
        }
    }

    private fun insertTab() {
        val textArea = getControl()
        val tabOrSpaces = textArea.tabIndentation()

        if (textArea.selection.length == 0) {
            // No selection. Just add the tab (or spaces)
            textArea.replaceSelection(tabOrSpaces)

        } else {

            extendSelectionToStartOfLine()
            val selection = textArea.selection

            var extraNL = ""
            var selectedText = textArea.selectedText
            if (selectedText.endsWith("\n")) {
                selectedText = selectedText.substring(0, selectedText.length - 1)
                extraNL = "\n"
            }
            val lines = selectedText.split('\n').map { tabOrSpaces + it }
            val replacement = lines.joinToString(separator = "\n") + extraNL

            textArea.replaceSelection(replacement)
            textArea.selectRange(selection.start, selection.end + tabOrSpaces.length * lines.size)
        }

    }

    /**
     * Unindents the current line, or selection. (Shift+Tab)
     */
    private fun unindent() {
        val textArea = getControl()
        val spaces = " ".repeat(textArea.indentSize)

        extendSelectionToStartOfLine()
        val selection = textArea.selection

        val selectedText = textArea.selectedText

        var deleted = 0
        val lines = selectedText.split('\n').map {
            if (it.startsWith("\t")) {
                deleted += 1
                it.substring(1)
            } else if (it.startsWith(spaces)) {
                deleted += spaces.length
                it.substring(spaces.length)
            } else {
                it
            }
        }
        val replacement = lines.joinToString(separator = "\n")
        textArea.replaceSelection(replacement)

        textArea.selectRange(selection.start - if (selectedText.startsWith("\t")) 1 else 0,
                selection.end - deleted)
    }

    private fun deleteChar(previous: Boolean) {
        skin.deleteChar(previous)
    }

    private fun deleteFromLineStart() {
        val textArea = getControl()
        val end = textArea.caretPosition

        if (end > 0) {
            skin.lineStart(false)
            val start = textArea.caretPosition
            if (end > start) {
                replaceText(start, end, "")
            }
        }
    }

    private fun replaceText(start: Int, end: Int, txt: String) {
        control.replaceText(start, end, txt)
    }

    companion object {

        val TEDI_AREA_BINDINGS: MutableList<KeyBinding> = ArrayList()

        init {

            TEDI_AREA_BINDINGS.add(KeyBinding(HOME, KEY_PRESSED, "LineStart"))
            TEDI_AREA_BINDINGS.add(KeyBinding(END, KEY_PRESSED, "LineEnd"))
            TEDI_AREA_BINDINGS.add(KeyBinding(UP, KEY_PRESSED, "PreviousLine"))
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_UP, KEY_PRESSED, "PreviousLine"))
            TEDI_AREA_BINDINGS.add(KeyBinding(DOWN, KEY_PRESSED, "NextLine"))
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_DOWN, KEY_PRESSED, "NextLine"))
            TEDI_AREA_BINDINGS.add(KeyBinding(PAGE_UP, KEY_PRESSED, "PreviousPage"))
            TEDI_AREA_BINDINGS.add(KeyBinding(PAGE_DOWN, KEY_PRESSED, "NextPage"))
            TEDI_AREA_BINDINGS.add(KeyBinding(ENTER, KEY_PRESSED, "InsertNewLine"))
            TEDI_AREA_BINDINGS.add(KeyBinding(TAB, KEY_PRESSED, "TraverseOrInsertTab"))
            TEDI_AREA_BINDINGS.add(KeyBinding(TAB, KEY_PRESSED, "Unindent").shift())

            TEDI_AREA_BINDINGS.add(KeyBinding(HOME, KEY_PRESSED, "SelectLineStart").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(END, KEY_PRESSED, "SelectLineEnd").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(UP, KEY_PRESSED, "SelectPreviousLine").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_UP, KEY_PRESSED, "SelectPreviousLine").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(DOWN, KEY_PRESSED, "SelectNextLine").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_DOWN, KEY_PRESSED, "SelectNextLine").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(PAGE_UP, KEY_PRESSED, "SelectPreviousPage").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(PAGE_DOWN, KEY_PRESSED, "SelectNextPage").shift())

            TEDI_AREA_BINDINGS.add(KeyBinding(RIGHT, KEY_PRESSED, "Right"))
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_RIGHT, KEY_PRESSED, "Right"))
            TEDI_AREA_BINDINGS.add(KeyBinding(LEFT, KEY_PRESSED, "Left"))
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_LEFT, KEY_PRESSED, "Left"))
            TEDI_AREA_BINDINGS.add(KeyBinding(UP, KEY_PRESSED, "Home"))
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_UP, KEY_PRESSED, "Home"))
            TEDI_AREA_BINDINGS.add(KeyBinding(HOME, KEY_PRESSED, "Home"))
            TEDI_AREA_BINDINGS.add(KeyBinding(DOWN, KEY_PRESSED, "End"))
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_DOWN, KEY_PRESSED, "End"))
            TEDI_AREA_BINDINGS.add(KeyBinding(END, KEY_PRESSED, "End"))
            TEDI_AREA_BINDINGS.add(KeyBinding(ENTER, KEY_PRESSED, "Fire"))
            // deletion
            TEDI_AREA_BINDINGS.add(KeyBinding(BACK_SPACE, KEY_PRESSED, "DeletePreviousChar"))
            TEDI_AREA_BINDINGS.add(KeyBinding(DELETE, KEY_PRESSED, "DeleteNextChar"))
            // cut/copy/paste
            TEDI_AREA_BINDINGS.add(KeyBinding(CUT, KEY_PRESSED, "Cut"))
            TEDI_AREA_BINDINGS.add(KeyBinding(DELETE, KEY_PRESSED, "Cut").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(COPY, KEY_PRESSED, "Copy"))
            TEDI_AREA_BINDINGS.add(KeyBinding(PASTE, KEY_PRESSED, "Paste"))
            TEDI_AREA_BINDINGS.add(KeyBinding(INSERT, KEY_PRESSED, "Paste").shift())
            // selection
            TEDI_AREA_BINDINGS.add(KeyBinding(RIGHT, KEY_PRESSED, "SelectRight").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_RIGHT, KEY_PRESSED, "SelectRight").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(LEFT, KEY_PRESSED, "SelectLeft").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_LEFT, KEY_PRESSED, "SelectLeft").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(UP, KEY_PRESSED, "SelectHome").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_UP, KEY_PRESSED, "SelectHome").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(DOWN, KEY_PRESSED, "SelectEnd").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_DOWN, KEY_PRESSED, "SelectEnd").shift())

            TEDI_AREA_BINDINGS.add(KeyBinding(BACK_SPACE, KEY_PRESSED, "DeletePreviousChar").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(DELETE, KEY_PRESSED, "DeleteNextChar").shift())

            TEDI_AREA_BINDINGS.add(KeyBinding(HOME, KEY_PRESSED, "SelectHome").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(END, KEY_PRESSED, "SelectEnd").shift())

            TEDI_AREA_BINDINGS.add(KeyBinding(HOME, KEY_PRESSED, "Home").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(END, KEY_PRESSED, "End").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(LEFT, KEY_PRESSED, "LeftWord").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_LEFT, KEY_PRESSED, "LeftWord").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(RIGHT, KEY_PRESSED, "RightWord").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_RIGHT, KEY_PRESSED, "RightWord").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(H, KEY_PRESSED, "DeletePreviousChar").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(DELETE, KEY_PRESSED, "DeleteNextWord").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(BACK_SPACE, KEY_PRESSED, "DeletePreviousWord").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(X, KEY_PRESSED, "Cut").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(C, KEY_PRESSED, "Copy").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(INSERT, KEY_PRESSED, "Copy").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(V, KEY_PRESSED, "Paste").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(HOME, KEY_PRESSED, "SelectHome").shortcut().shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(END, KEY_PRESSED, "SelectEnd").shortcut().shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(LEFT, KEY_PRESSED, "SelectLeftWord").shortcut().shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_LEFT, KEY_PRESSED, "SelectLeftWord").shortcut().shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(RIGHT, KEY_PRESSED, "SelectRightWord").shortcut().shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(KP_RIGHT, KEY_PRESSED, "SelectRightWord").shortcut().shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(A, KEY_PRESSED, "SelectAll").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(BACK_SLASH, KEY_PRESSED, "Unselect").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(Z, KEY_PRESSED, "Undo").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(Z, KEY_PRESSED, "Redo").shortcut().shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(Y, KEY_PRESSED, "Redo").shortcut())

            // Any other key press first goes to normal text input
            // Note this is KEY_TYPED because otherwise the character is not available in the event.
            TEDI_AREA_BINDINGS.add(KeyBinding(null, KeyEvent.KEY_TYPED, "InputCharacter")
                    .alt(OptionalBoolean.ANY)
                    .shift(OptionalBoolean.ANY)
                    .ctrl(OptionalBoolean.ANY)
                    .meta(OptionalBoolean.ANY))

            // Traversal Bindings
            TEDI_AREA_BINDINGS.add(KeyBinding(TAB, "TraverseNext"))
            TEDI_AREA_BINDINGS.add(KeyBinding(TAB, "TraversePrevious").shift())
            TEDI_AREA_BINDINGS.add(KeyBinding(TAB, "TraverseNext").shortcut())
            TEDI_AREA_BINDINGS.add(KeyBinding(TAB, "TraversePrevious").shift().shortcut())

            // The following keys are forwarded to the parent container
            TEDI_AREA_BINDINGS.add(KeyBinding(ESCAPE, "Cancel"))
            TEDI_AREA_BINDINGS.add(KeyBinding(F10, "ToParent"))

            // However, we want to consume other key press / release events too, for
            // things that would have been handled by the InputCharacter normally
            TEDI_AREA_BINDINGS.add(KeyBinding(null, KEY_PRESSED, "Consume"))

        }

    }
}
