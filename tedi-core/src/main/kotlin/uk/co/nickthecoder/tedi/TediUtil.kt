package uk.co.nickthecoder.tedi

import javafx.scene.control.Button
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

fun Button.loadGraphic(klass: Class<*>, name: String) {
    graphic = imageViewResource(klass, name)
    // Fallback if resource wan't found
    if (graphic == null && text.isEmpty()) {
        text = name
    }
}
