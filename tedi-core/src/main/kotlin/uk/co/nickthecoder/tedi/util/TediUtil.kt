/*
Tedi
Copyright (C) 2018 Nick Robinson

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2 only, as
published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
package uk.co.nickthecoder.tedi.util

import com.sun.javafx.scene.control.skin.ComboBoxPopupControl.FakeFocusTextField
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.css.*
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.ButtonBase
import javafx.scene.control.ComboBox
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.text.Font
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

fun clamp(min: Double, value: Double, max: Double): Double {
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
    val field = this::class.java.findField(fieldName)
            ?: throw IllegalArgumentException("Field $fieldName not found in ${this::class.java.name}")
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
    val field = this::class.java.findField(fieldName)
            ?: throw IllegalArgumentException("Field $fieldName not found in ${this::class.java.name}")
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


//--------------------------------------------------------------------------
// CSS Boiler plate cruft removal functions
//--------------------------------------------------------------------------

inline fun <reified N : Styleable> createPaintCssMetaData(
        property: String,
        defaultValue: Paint = Color.WHITE,
        crossinline getter: (N) -> StyleableObjectProperty<Paint?>) =
        createCssMetaData<N, Paint?>(property, StyleConverter.getPaintConverter(), defaultValue, getter)

inline fun <reified N : Styleable> createBooleanCssMetaData(
        property: String,
        defaultValue: Boolean = false,
        crossinline getter: (N) -> StyleableBooleanProperty) =
        object : CssMetaData<N, Boolean>(property, StyleConverter.getBooleanConverter(), defaultValue) {
            override fun getStyleableProperty(styleable: N) = getter(styleable)
            override fun isSettable(styleable: N): Boolean = !getStyleableProperty(styleable).isBound
        }

inline fun <reified N : Styleable> createFontCssMetaData(
        property: String,
        crossinline getter: (N) -> StyleableObjectProperty<Font>) =
        createCssMetaData<N, Font>(property, StyleConverter.getFontConverter(), Font.getDefault(), getter)


inline fun <reified N : Styleable, T> createCssMetaData(
        property: String,
        converter: StyleConverter<*, T>,
        defaultValue: T,
        crossinline getter: (N) -> StyleableObjectProperty<T>): CssMetaData<N, T> {

    return object : CssMetaData<N, T>(property, converter, defaultValue) {
        override fun getStyleableProperty(styleable: N) = getter(styleable)
        override fun isSettable(styleable: N): Boolean = !getStyleableProperty(styleable).isBound
    }
}

fun <T> Any.createStyleable(name: String, value: T, meta: CssMetaData<out Styleable, T>) = object : StyleableObjectProperty<T>(value) {
    override fun getBean() = this@createStyleable
    override fun getName() = name
    override fun getCssMetaData() = meta
}

fun Any.createStyleable(name: String, value: Boolean, meta: CssMetaData<out Styleable, Boolean>): StyleableBooleanProperty = object : StyleableBooleanProperty(value) {
    override fun getBean() = this@createStyleable
    override fun getName() = name
    override fun getCssMetaData() = meta
}
