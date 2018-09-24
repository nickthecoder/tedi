package uk.co.nickthecoder.tedi

import com.sun.javafx.scene.control.skin.ComboBoxPopupControl.FakeFocusTextField
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.ButtonBase
import javafx.scene.control.ComboBox
import javafx.scene.control.TextInputControl
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import java.lang.reflect.Field


private fun getOs(): String = System.getProperty("os.name").toLowerCase()

val isMac by lazy { getOs().startsWith("mac") }
val isLinux by lazy { getOs().startsWith("linux") }
val isWindows by lazy { getOs().startsWith("windows") }

val javaFXVersion by lazy { System.getProperties().getProperty("javafx.runtime.version") }

/**
 * Simple utility function which clamps the given value to be strictly
 * between the min and max values.
 */
fun clamp(min: Int, value: Int, max: Int): Int {
    if (value < min) return min
    if (value > max) return max
    return value
}

fun imageResource(klass: Class<*>, name: String): Image? {
    val imageStream = klass.getResourceAsStream(name)
    return if (imageStream == null) null else Image(imageStream)
}

fun imageViewResource(klass: Class<*>, name: String): ImageView? {
    val image = imageResource(klass, name)
    return if (image == null) null else ImageView(image)
}

fun ButtonBase.loadGraphic(klass: Class<*>, name: String) {
    graphic = imageViewResource(klass, name)
    // Fallback if resource wan't found
    if (graphic == null && text.isEmpty()) {
        text = name
    }
}

/**
 * Extension function for all [TextInputControl]s, for compatibility with [TediArea.lineColumnFor]
 */
fun TextInputControl.lineColumnFor(position: Int): Pair<Int, Int> {

    // Call the TediArea version if we can (this may be more efficient if/when I optimise it)
    if (this is TediArea) return lineColumnFor(position)

    var count = 0
    var i = 0
    val paragraphs = text.split("\n")
    for (p in paragraphs) {
        if (count + p.length >= position) {
            return Pair(i, position - count)
        }
        count += p.length + 1 // 1 for the new line character
        i++
    }
    return Pair(i, position - count)
}

/**
 * Extension function for all [TextInputControl]s, for compatibility with [TediArea.positionFor]
 */
fun TextInputControl.positionFor(line: Int, column: Int): Int {

    // Call the TediArea version if we can (this may be more efficient if/when I optimise it)
    if (this is TediArea) return positionFor(line, column)

    val paragraphs = text.split("\n")
    var lineStart = 0
    for ((i, p) in paragraphs.withIndex()) {
        if (i >= line) {
            break
        }
        lineStart += p.length + 1 // 1 for the new line character
    }

    if (line >= 0 && line < paragraphs.size) {
        return lineStart + clamp(0, paragraphs[line].length, column)
    } else {
        return lineStart
    }

}

/**
 * If the scene property for the node is null, then set up a listener, and focus once the scene is
 * set. If already set, then no listener is created, and requestFocus is performed straight away.
 */
fun Node.requestFocusOnSceneAvailable() {
    if (scene == null) {
        val listener = object : ChangeListener<Scene> {
            override fun changed(observable: ObservableValue<out Scene>?, oldValue: Scene?, newValue: Scene?) {
                if (newValue != null) {
                    sceneProperty().removeListener(this)
                    requestFocus()
                }
            }
        }
        sceneProperty().addListener(listener)
    } else {
        requestFocus()
    }
}

fun Node.onSceneAvailable(action: () -> Unit) {
    if (scene == null) {
        val listener = object : ChangeListener<Scene> {
            override fun changed(observable: ObservableValue<out Scene>?, oldValue: Scene?, newValue: Scene?) {
                if (newValue != null) {
                    sceneProperty().removeListener(this)
                    action()
                }
            }
        }
        sceneProperty().addListener(listener)
    } else {
        action()
    }
}

/**
 * A work-around for a JavaFX 8 bug :
 * https://stackoverflow.com/questions/40239400/javafx-8-missing-caret-in-switch-editable-combobox
 *
 * The bodge is only performed if [javaFXVersion] is "8.xxx", to avoid issues with using the non-standard class
 * com.sun.javafx.scene.control.skin.ComboBoxPopupControl
 *
 * Hopefully it will be fixed in version 9!
 */
fun ComboBox<*>.requestFocusWithCaret() {
    // Without this first line, I could end up with TWO ComboBoxes appearing to have focus at the same time!
    requestFocus()

    if (javaFXVersion.startsWith("8.")) {
        try {
            val theEditor = editor
            if (theEditor is FakeFocusTextField) {
                theEditor.setFakeFocus(true)
            }
        } catch (e: Exception) {

        }
    }
}


/**
 * Uses reflection to set a private field
 */
fun Any.setPrivateField(fieldName: String, value: Any?) {
    val field = this::class.java.findField(fieldName) ?: throw IllegalArgumentException("Field $fieldName not found in ${this::class.java.name}")
    // I'm not sure if the Field returned by [Class.getDeclaredField] is a new instance or not,
    // but I'll err on the safe side, and reset isAccessible to its original state.
    val oldAccessible = field.isAccessible

    try {
        field.isAccessible = true
        field.set(this, value)
    } finally {
        try {
            field.isAccessible = oldAccessible
        } catch (e: Exception) {
        }
    }
}

fun Any.getPrivateField(fieldName: String): Any? {
    val field = this::class.java.findField(fieldName) ?: throw IllegalArgumentException("Field $fieldName not found in ${this::class.java.name}")
    // I'm not sure if the Field returned by [Class.getDeclaredField] is a new instance or not,
    // but I'll err on the safe side, and reset isAccessible to its original state.
    val oldAccessible = field.isAccessible

    try {
        field.isAccessible = true
        return field.get(this)
    } finally {
        try {
            field.isAccessible = oldAccessible
        } catch (e: Exception) {
        }
    }
}

/**
 * Go up the class hierarchy looking for a field with a given name.
 * Unlike [Class.getField], this will find private fields as well as public ones.
 * Unlike [Class.getDeclaredField], this will find fields declared in a super class too.
 *
 * If no such field is found, then null is returned.
 */
fun Class<*>.findField(fieldName: String): Field? {
    var klass: Class<out Any?>? = this
    do {
        try {
            klass?.getDeclaredField(fieldName)?.let { return it }
        } catch (e: NoSuchFieldException) {
        }
        klass = klass?.superclass
    } while (klass != null)

    return null
}

/**
 * Creates a new list, and adds a list, and another set of extras.
 * I use this for CSS meta data, where the super class has a list, and the sub class wants to extend that list
 * with additional items.
 */
fun <E> extendList(parent: List<E>, vararg extra: E): List<E> {
    val result = mutableListOf<E>()
    result.addAll(parent)
    result.addAll(listOf(*extra))
    return result
}

/**
 * A little utility method for stripping out unwanted characters.

 * @param text
 * *
 * @param stripNewlines
 * *
 * @param stripTabs
 * *
 * @return The string after having the unwanted characters stripped out.
 */
internal fun filterInput(text: String, stripNewlines: Boolean, stripTabs: Boolean): String {

    var result = text

    // Most of the time, when text is inserted, there are no illegal
    // characters. So we'll do a "cheap" check for illegal characters.
    // If we find one, we'll do a longer replace algorithm. In the
    // case of illegal characters, this may at worst be an O(2n) solution.
    // Strip out any characters that are outside the printed range
    if (containsInvalidCharacters(result, stripNewlines, stripTabs)) {
        val s = StringBuilder(result.length)
        for (i in 0..result.length - 1) {
            val c = result[i]
            if (!isInvalidCharacter(c, stripNewlines, stripTabs)) {
                s.append(c)
            }
        }
        result = s.toString()
    }
    return result
}

internal fun containsInvalidCharacters(text: String, newlineIllegal: Boolean, tabIllegal: Boolean): Boolean {
    for (i in 0..text.length - 1) {
        val c = text[i]
        if (isInvalidCharacter(c, newlineIllegal, tabIllegal)) return true
    }
    return false
}

private fun isInvalidCharacter(c: Char, newlineIllegal: Boolean, tabIllegal: Boolean): Boolean {
    if (c.toInt() == 0x7F) return true
    if (c.toInt() == 0xA) return newlineIllegal
    if (c.toInt() == 0x9) return tabIllegal
    if (c.toInt() < 0x20) return true
    return false
}
