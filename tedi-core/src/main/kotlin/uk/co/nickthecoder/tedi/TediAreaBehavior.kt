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

import com.sun.javafx.application.PlatformImpl
import com.sun.javafx.scene.control.skin.TextInputControlSkin
import com.sun.javafx.scene.text.HitInfo
import javafx.application.ConditionalFeature
import javafx.beans.InvalidationListener
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.geometry.NodeOrientation
import javafx.scene.control.ContextMenu
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import uk.co.nickthecoder.tedi.javafx.BehaviorBase
import uk.co.nickthecoder.tedi.javafx.KeyBinding
import java.text.Bidi
import java.util.*

class TediAreaBehavior(val control: TediArea)

    : BehaviorBase<TediArea>(control, TEXT_AREA_BINDINGS) {

    private val skin: TediAreaSkin
        get() = control.skin as TediAreaSkin


    private val contextMenu = ContextMenu()

    /**
     * Used to keep track of the most recent key event. This is used when
     * handling InputCharacter actions.
     */
    private var lastEvent: KeyEvent? = null

    private val textListener = InvalidationListener { observable -> invalidateBidi() }

    init {
        // Register for change events
        control.focusedProperty().addListener(
                object : ChangeListener<Boolean> {
                    override fun changed(observable: ObservableValue<out Boolean>, oldValue: Boolean?, newValue: Boolean?) {
                        // NOTE: The code in this method is *almost* and exact copy of what is in TextFieldBehavior.
                        // The only real difference is that TextFieldBehavior selects all the text when the control
                        // receives focus (when not gained by mouse click), whereas TextArea doesn't, and also the
                        // TextArea doesn't lose selection on focus lost, whereas the TextField does.
                        if (control.isFocused()) {
                            if (!focusGainedByMouseClick) {
                                setCaretAnimating(true)
                            }
                        } else {
                            focusGainedByMouseClick = false
                            setCaretAnimating(false)
                        }
                    }
                })

        control.textProperty().addListener(textListener)

    }

    override fun dispose() {
        control.textProperty().removeListener(textListener)
        super.dispose()
    }

    /**************************************************************************
     * Key handling implementation                                            *
     */

    /**
     * Records the last KeyEvent we saw.
     * @param e
     */
    override fun callActionForEvent(e: KeyEvent) {
        lastEvent = e
        super.callActionForEvent(e)
    }

    public override fun callAction(name: String) {

        var name = name
        val textInputControl = getControl()

        var done = false

        if (textInputControl.isEditable) {

            setEditing(true)
            done = true
            if ("InputCharacter" == name)
                lastEvent?.let { defaultKeyTyped(it) }
            else if ("Cut" == name)
                cut()
            else if ("Paste" == name)
                paste()
            else if ("DeleteFromLineStart" == name)
                deleteFromLineStart()
            else if ("DeletePreviousChar" == name)
                deletePreviousChar()
            else if ("DeleteNextChar" == name)
                deleteNextChar()
            else if ("DeletePreviousWord" == name)
                deletePreviousWord()
            else if ("DeleteNextWord" == name)
                deleteNextWord()
            else if ("DeleteSelection" == name)
                deleteSelection()
            else if ("Undo" == name)
                textInputControl.undo()
            else if ("Redo" == name)
                textInputControl.redo()
            else if ("InsertNewLine" == name)
                insertNewLine()
            else if ("TraverseOrInsertTab" == name)
                insertTab()
            else {
                done = false
            }
            setEditing(false)
        }

        if (!done) {
            done = true
            if ("Copy" == name)
                textInputControl.copy()
            else if ("SelectBackward" == name)
                textInputControl.selectBackward()
            else if ("SelectForward" == name)
                textInputControl.selectForward()
            else if ("SelectLeft" == name)
                selectLeft()
            else if ("SelectRight" == name)
                selectRight()
            else if ("PreviousWord" == name)
                previousWord()
            else if ("NextWord" == name)
                nextWord()
            else if ("LeftWord" == name)
                leftWord()
            else if ("RightWord" == name)
                rightWord()
            else if ("SelectPreviousWord" == name)
                selectPreviousWord()
            else if ("SelectNextWord" == name)
                selectNextWord()
            else if ("SelectLeftWord" == name)
                selectLeftWord()
            else if ("SelectRightWord" == name)
                selectRightWord()
            else if ("SelectWord" == name)
                selectWord()
            else if ("SelectAll" == name)
                textInputControl.selectAll()
            else if ("Home" == name)
                textInputControl.home()
            else if ("End" == name)
                textInputControl.end()
            else if ("Forward" == name)
                textInputControl.forward()
            else if ("Backward" == name)
                textInputControl.backward()
            else if ("Right" == name)
                nextCharacterVisually(true)
            else if ("Left" == name)
                nextCharacterVisually(false)
            else if ("Fire" == name)
                lastEvent?.let { fire(it) }
            else if ("Cancel" == name)
                lastEvent?.let { cancelEdit(it) }
            else if ("Unselect" == name)
                textInputControl.deselect()
            else if ("SelectHome" == name)
                selectHome()
            else if ("SelectEnd" == name)
                selectEnd()
            else if ("SelectHomeExtend" == name)
                selectHomeExtend()
            else if ("SelectEndExtend" == name)
                selectEndExtend()
            else if ("ToParent" == name)
                lastEvent?.let { forwardToParent(it) }
            else if ("UseVK" == name && PlatformImpl.isSupported(ConditionalFeature.VIRTUAL_KEYBOARD)) {
                (textInputControl.skin as TextInputControlSkin<*, *>).toggleUseVK()

                // From TextAreaBehavior
            } else if ("LineStart" == name)
                lineStart(false, false)
            else if ("LineEnd" == name)
                lineEnd(false, false)
            else if ("SelectLineStart" == name)
                lineStart(true, false)
            else if ("SelectLineStartExtend" == name)
                lineStart(true, true)
            else if ("SelectLineEnd" == name)
                lineEnd(true, false)
            else if ("SelectLineEndExtend" == name)
                lineEnd(true, true)
            else if ("PreviousLine" == name)
                skin.previousLine(false)
            else if ("NextLine" == name)
                skin.nextLine(false)
            else if ("SelectPreviousLine" == name)
                skin.previousLine(true)
            else if ("SelectNextLine" == name)
                skin.nextLine(true)
            else if ("ParagraphStart" == name)
                skin.paragraphStart(true, false)
            else if ("ParagraphEnd" == name)
                skin.paragraphEnd(true, isWindows(), false)
            else if ("SelectParagraphStart" == name)
                skin.paragraphStart(true, true)
            else if ("SelectParagraphEnd" == name)
                skin.paragraphEnd(true, isWindows(), true)
            else if ("PreviousPage" == name)
                skin.previousPage(false)
            else if ("NextPage" == name)
                skin.nextPage(false)
            else if ("SelectPreviousPage" == name)
                skin.previousPage(true)
            else if ("SelectNextPage" == name)
                skin.nextPage(true)
            else if ("TraverseOrInsertTab" == name) {
                // RT-40312: Non-editabe mode means traverse instead of insert.
                name = "TraverseNext"
                done = false
            } else {
                done = false
            }
        }

        if (!done) {
            super.callAction(name)
        }
    }


    private fun insertNewLine() {
        control.replaceSelection("\n")
    }

    override fun mousePressed(e: MouseEvent?) {
        super.mousePressed(e)
        // We never respond to events if disabled
        if (!control.isDisabled) {
            // If the text field doesn't have focus, then we'll attempt to set
            // the focus and we'll indicate that we gained focus by a mouse
            // click, TODO which will then NOT honor the selectOnFocus variable
            // of the textInputControl
            if (!control.isFocused) {
                focusGainedByMouseClick = true
                control.requestFocus()
            }

            // stop the caret animation
            setCaretAnimating(false)
            // only if there is no selection should we see the caret
            //            setCaretOpacity(if (textInputControl.dot == textInputControl.mark) then 1.0 else 0.0);

            // if the primary button was pressed
            if (e!!.button == MouseButton.PRIMARY && !(e.isMiddleButtonDown || e.isSecondaryButtonDown)) {
                val hit = skin.getIndex(e.x, e.y)
                val i = com.sun.javafx.scene.control.skin.Utils.getHitInsertionIndex(hit, control.textProperty().valueSafe)
                //                 int i = skin.getInsertionPoint(e.getX(), e.getY());
                val anchor = control.anchor
                val caretPosition = control.caretPosition
                if (e.clickCount < 2 && (e.isSynthesized || anchor != caretPosition && (i > anchor && i < caretPosition || i < anchor && i > caretPosition))) {
                    // if there is a selection, then we will NOT handle the
                    // press now, but will defer until the release. If you
                    // select some text and then press down, we change the
                    // caret and wait to allow you to drag the text (TODO).
                    // When the drag concludes, then we handle the click

                    deferClick = true
                    // TODO start a timer such that after some millis we
                    // switch into text dragging mode, change the cursor
                    // to indicate the text can be dragged, etc.
                } else if (!(e.isControlDown || e.isAltDown || e.isShiftDown || e.isMetaDown || e.isShortcutDown)) {
                    when (e.clickCount) {
                        1 -> skin.positionCaret(hit, false, false)
                        2 -> mouseDoubleClick(hit)
                        3 -> mouseTripleClick(hit)
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
                    if (isMac()) {
                        control.extendSelection(i)
                    } else {
                        skin.positionCaret(hit, true, false)
                    }
                }
                //                 skin.setForwardBias(hit.isLeading());
                //                if (textInputControl.editable)
                //                    displaySoftwareKeyboard(true);
            }
            if (contextMenu.isShowing) {
                contextMenu.hide()
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
                skin.positionCaret(skin.getIndex(e.x, e.y), true, false)
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
                skin.positionCaret(skin.getIndex(e!!.x, e.y), shiftDown, false)
                shiftDown = false
            }
            setCaretAnimating(true)
        }
    }

    override fun contextMenuRequested(e: ContextMenuEvent?) {
        val textArea = getControl()

        if (contextMenu.isShowing) {
            contextMenu.hide()
        } else if (textArea.contextMenu == null) {
            var screenX = e!!.screenX
            var screenY = e.screenY
            var sceneX = e.sceneX

            if (IS_TOUCH_SUPPORTED) {
                /*
                var menuPos: Point2D?
                if (textArea.selection.length == 0) {
                    skin.positionCaret(skin.getIndex(e.x, e.y), false, false)
                    menuPos = skin.getMenuPosition()
                } else {
                    menuPos = skin.getMenuPosition()
                    if (menuPos != null && (menuPos.x <= 0 || menuPos.y <= 0)) {
                        skin.positionCaret(skin.getIndex(e.x, e.y), false, false)
                        menuPos = skin.getMenuPosition()
                    }
                }

                if (menuPos != null) {
                    val p = getControl().localToScene(menuPos)
                    val scene = getControl().scene
                    val window = scene.window
                    val location = Point2D(window.x + scene.x + p.x,
                            window.y + scene.y + p.y)
                    screenX = location.x
                    sceneX = p.x
                    screenY = location.y
                }
                */
            }

            val menuWidth = contextMenu.prefWidth(-1.0)
            val menuX = screenX - if (IS_TOUCH_SUPPORTED) menuWidth / 2.0 else 0.0
            val currentScreen = com.sun.javafx.util.Utils.getScreenForPoint(screenX, 0.0)
            val bounds = currentScreen.bounds

            if (menuX < bounds.minX) {
                getControl().properties.put("CONTEXT_MENU_SCREEN_X", screenX)
                getControl().properties.put("CONTEXT_MENU_SCENE_X", sceneX)
                contextMenu.show(getControl(), bounds.minX, screenY)
            } else if (screenX + menuWidth > bounds.maxX) {
                val leftOver = menuWidth - (bounds.maxX - screenX)
                getControl().properties.put("CONTEXT_MENU_SCREEN_X", screenX)
                getControl().properties.put("CONTEXT_MENU_SCENE_X", sceneX)
                contextMenu.show(getControl(), screenX - leftOver, screenY)
            } else {
                getControl().properties.put("CONTEXT_MENU_SCREEN_X", 0)
                getControl().properties.put("CONTEXT_MENU_SCENE_X", 0)
                contextMenu.show(getControl(), menuX, screenY)
            }
        }

        e!!.consume()
    }

    protected fun setCaretAnimating(play: Boolean) {
        skin.setCaretAnimating(play)
    }

    protected fun mouseDoubleClick(hit: HitInfo) {
        val textArea = getControl()
        textArea.previousWord()
        if (isWindows()) {
            textArea.selectNextWord()
        } else {
            textArea.selectEndOfNextWord()
        }
    }

    protected fun mouseTripleClick(hit: HitInfo) {
        // select the line
        skin.paragraphStart(false, false)
        skin.paragraphEnd(false, isWindows(), true)
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

        // Filter out control keys except control+Alt on PC or Alt on Mac
        if (event.isControlDown || event.isAltDown || isMac() && event.isMetaDown) {
            if (!((event.isControlDown || isMac()) && event.isAltDown)) return
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
            scrollCharacterToVisible(start)
        }
    }

    private var bidi: Bidi? = null
    private var mixed: Boolean? = null
    private var rtlText: Boolean? = null

    private fun invalidateBidi() {
        bidi = null
        mixed = null
        rtlText = null
    }

    private fun getBidi(): Bidi {
        if (bidi == null) {
            bidi = Bidi(control.textProperty().getValueSafe(),
                    if (control.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT)
                        Bidi.DIRECTION_RIGHT_TO_LEFT
                    else
                        Bidi.DIRECTION_LEFT_TO_RIGHT)
        }
        return bidi!!
    }

    protected fun isMixed(): Boolean {
        if (mixed == null) {
            mixed = getBidi().isMixed
        }
        return mixed!!
    }

    protected fun isRTLText(): Boolean {
        if (rtlText == null) {
            val bidi = getBidi()
            rtlText = bidi.isRightToLeft || isMixed() && control.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT
        }
        return rtlText!!
    }


    private fun nextCharacterVisually(moveRight: Boolean) {
        if (isMixed()) {
            val skin = control.getSkin() as TextInputControlSkin<*, *>
            skin.nextCharacterVisually(moveRight)
        } else if (moveRight != isRTLText()) {
            control.forward()
        } else {
            control.backward()
        }
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

    protected fun deletePreviousWord() {
        val textInputControl = getControl()
        val end = textInputControl.caretPosition

        if (end > 0) {
            textInputControl.previousWord()
            val start = textInputControl.caretPosition
            replaceText(start, end, "")
        }
    }

    protected fun deleteNextWord() {
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

    protected fun selectPreviousWord() {
        getControl().selectPreviousWord()
    }

    protected fun selectNextWord() {
        val textInputControl = getControl()
        if (isMac() || isLinux()) {
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

    protected fun selectWord() {
        val textInputControl = getControl()
        textInputControl.previousWord()
        if (isWindows()) {
            textInputControl.selectNextWord()
        } else {
            textInputControl.selectEndOfNextWord()
        }
    }

    protected fun previousWord() {
        getControl().previousWord()
    }

    protected fun nextWord() {
        val textInputControl = getControl()
        if (isMac() || isLinux()) {
            textInputControl.endOfNextWord()
        } else {
            textInputControl.nextWord()
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

    protected fun fire(event: KeyEvent) {}

    protected fun cancelEdit(event: KeyEvent) {
        forwardToParent(event)
    }

    protected fun forwardToParent(event: KeyEvent) {
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

    private fun insertTab() {
        val textArea = getControl()
        textArea.replaceSelection("\t")
    }

    protected fun deleteChar(previous: Boolean) {
        skin.deleteChar(previous)
    }

    protected fun deleteFromLineStart() {
        val textArea = getControl()
        val end = textArea.caretPosition

        if (end > 0) {
            lineStart(false, false)
            val start = textArea.caretPosition
            if (end > start) {
                replaceText(start, end, "")
            }
        }
    }

    private fun lineStart(select: Boolean, extendSelection: Boolean) {
        skin.lineStart(select, extendSelection)
    }

    private fun lineEnd(select: Boolean, extendSelection: Boolean) {
        skin.lineEnd(select, extendSelection)
    }

    protected fun scrollCharacterToVisible(index: Int) {
        skin.scrollCharacterToVisible(index)
    }

    protected fun replaceText(start: Int, end: Int, txt: String) {
        control.replaceText(start, end, txt)
    }

    /**
     * If the focus is gained via response to a mouse click, then we don't
     * want to select all the text even if selectOnFocus is true.
     */
    private var focusGainedByMouseClick = false // TODO!!
    private var shiftDown = false
    private var deferClick = false


    private var editing = false

    protected fun setEditing(b: Boolean) {
        editing = b
    }

    fun isEditing(): Boolean {
        return editing
    }

    // TODO Do something better?
    fun isMac() = false

    fun isLinux() = true
    fun isWindows() = false

    companion object {

        val TEXT_AREA_BINDINGS: MutableList<KeyBinding> = ArrayList()

        init {
            // However, we want to consume other key press / release events too, for
            // things that would have been handled by the InputCharacter normally
            TEXT_AREA_BINDINGS.add(KeyBinding(null, KEY_PRESSED, "Consume"))

            TEXT_AREA_BINDINGS.add(KeyBinding(HOME, KEY_PRESSED, "LineStart")) // changed
            TEXT_AREA_BINDINGS.add(KeyBinding(END, KEY_PRESSED, "LineEnd")) // changed
            TEXT_AREA_BINDINGS.add(KeyBinding(UP, KEY_PRESSED, "PreviousLine")) // changed
            TEXT_AREA_BINDINGS.add(KeyBinding(KP_UP, KEY_PRESSED, "PreviousLine")) // changed
            TEXT_AREA_BINDINGS.add(KeyBinding(DOWN, KEY_PRESSED, "NextLine")) // changed
            TEXT_AREA_BINDINGS.add(KeyBinding(KP_DOWN, KEY_PRESSED, "NextLine")) // changed
            TEXT_AREA_BINDINGS.add(KeyBinding(PAGE_UP, KEY_PRESSED, "PreviousPage")) // new
            TEXT_AREA_BINDINGS.add(KeyBinding(PAGE_DOWN, KEY_PRESSED, "NextPage")) // new
            TEXT_AREA_BINDINGS.add(KeyBinding(ENTER, KEY_PRESSED, "InsertNewLine")) // changed
            TEXT_AREA_BINDINGS.add(KeyBinding(TAB, KEY_PRESSED, "TraverseOrInsertTab")) // changed

            TEXT_AREA_BINDINGS.add(KeyBinding(HOME, KEY_PRESSED, "SelectLineStart").shift()) // changed
            TEXT_AREA_BINDINGS.add(KeyBinding(END, KEY_PRESSED, "SelectLineEnd").shift()) // changed
            TEXT_AREA_BINDINGS.add(KeyBinding(UP, KEY_PRESSED, "SelectPreviousLine").shift()) // changed
            TEXT_AREA_BINDINGS.add(KeyBinding(KP_UP, KEY_PRESSED, "SelectPreviousLine").shift()) // changed
            TEXT_AREA_BINDINGS.add(KeyBinding(DOWN, KEY_PRESSED, "SelectNextLine").shift()) // changed
            TEXT_AREA_BINDINGS.add(KeyBinding(KP_DOWN, KEY_PRESSED, "SelectNextLine").shift()) // changed
            TEXT_AREA_BINDINGS.add(KeyBinding(PAGE_UP, KEY_PRESSED, "SelectPreviousPage").shift()) // new
            TEXT_AREA_BINDINGS.add(KeyBinding(PAGE_DOWN, KEY_PRESSED, "SelectNextPage").shift()) // new
        }

    }
}