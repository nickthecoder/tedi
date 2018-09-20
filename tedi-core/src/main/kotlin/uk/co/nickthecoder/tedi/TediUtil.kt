package uk.co.nickthecoder.tedi

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.ButtonBase
import javafx.scene.control.TextInputControl
import javafx.scene.image.Image
import javafx.scene.image.ImageView

private fun getOs(): String = System.getProperty("os.name").toLowerCase()

val isMac by lazy { getOs().startsWith("mac") }
val isLinux by lazy { getOs().startsWith("linux") }
val isWindows by lazy { getOs().startsWith("windows") }


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
