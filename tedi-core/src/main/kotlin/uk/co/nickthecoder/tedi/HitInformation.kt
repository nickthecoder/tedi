package uk.co.nickthecoder.tedi

import javafx.scene.text.Text


/**
 * This is similar to JavaFX's non-public HitInfo class.
 * It holds information about where 2d point touches a [Text] object.
 * [charIndex] is the zero based index into the [Text's] text
 * [isLeading] true iff the point was to the left of the hit character's mid point.
 *
 * Use  [impl_hitTestChar]
 */
class HitInformation(var charIndex: Int, var isLeading: Boolean) {

    //init {
    //    println("Hit Information : $this")
    //}

    fun getInsertionIndex(): Int {
        return if (isLeading) charIndex else charIndex + 1
    }

    override fun toString() = "charIndex: $charIndex, isLeading: $isLeading"

}

private val tempText = Text()

/**
 * This is similar to the private api : [Text.impl_hitTestChar].
 * It works by first calculating the line that was hit (by calculating the line height from the
 * Text's height, and the number of lines of text)
 *
 * It then works out the character within that line, but first making a guess, and then refining the
 * guess my moving backwards/forwards a character at a time.
 * While refining the guess, a temporary Text object (with the same Font), has its text set to a
 * substring of the original line, and then the temporary Text object's width is compared to [y].
 * When the diff changes from +ve to -ve, we know that the answer lies between the two.
 * Whichever of these two guesses has the smallest diff is the winner.
 */
fun Text.hitTestChar(x: Double, y: Double): HitInformation {

    val normX = x - layoutX
    val normY = y - layoutY

    val lines = text.split("\n")
    val lineHeight = boundsInLocal.height / lines.size
    val lineNumber = (normY / lineHeight).toInt()
    if (lineNumber >= lines.size) {
        return HitInformation(text.length, false)
    }

    // println("Lines = ${lines.size} lineHeight=${lineHeight} textHeight=${text.boundsInLocal.height} Line Number $lineNumber}")
    var lineStartPosition = 0
    for (i in 0..lineNumber - 1) {
        lineStartPosition += lines[i].length + 1 // 1 for the new line character
    }

    val lineText = if (lineNumber < 0 || lineNumber >= lines.size) "" else lines[lineNumber]
    tempText.font = font
    tempText.text = lineText
    //println("REAL HitInfo : " + tempText.impl_hitTestChar(Point2D(normX, normY)))

    if (normX < 0) {
        return HitInformation(lineStartPosition, true)
    } else if (normX > tempText.boundsInLocal.width) {
        return HitInformation(lineStartPosition + lineText.length, true)
    } else {
        var guess = (normX / tempText.boundsInLocal.width * lineText.length).toInt()
        tempText.text = lineText.substring(0, guess)
        var diff = normX - tempText.boundsInLocal.width
        var previousDiff = diff

        if (diff < 0) {
            while (diff < 0 && guess > 0) {
                guess--
                tempText.text = lineText.substring(0, guess)
                previousDiff = diff
                diff = normX - tempText.boundsInLocal.width
            }
            if (guess < 0) {
                return HitInformation(lineStartPosition, true)
            } else {
                return HitInformation(lineStartPosition + guess, diff < -previousDiff)
            }
        } else {
            while (diff > 0 && guess < lineText.length) {
                guess++
                tempText.text = lineText.substring(0, guess)
                previousDiff = diff
                diff = normX - tempText.boundsInLocal.width
            }
            return HitInformation(lineStartPosition + guess - 1, diff < -previousDiff)
        }
    }
}


