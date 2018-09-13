package uk.co.nickthecoder.tedi

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.scene.control.ScrollPane
import javafx.scene.control.SkinBase
import javafx.scene.layout.Region
import javafx.scene.text.Text
import javafx.util.Duration
import java.lang.ref.WeakReference

class TediSkin(val tediView: TediView) : SkinBase<TediView>(tediView) {

    private val scrollPane = ScrollPane()
    private val content = TediContent()

    private val blink = SimpleBooleanProperty(this, "blink", true)
    private val caretBlinking = CaretBlinking(blink)

    private inner class TediContent : Region(), ListChangeListener<String> {
        val lines = mutableListOf<Text>()

        init {
            styleClass.add("content")

            tediView.document.lines().forEach {
                val text = Text(it)
                lines.add(text)
                children.add(text)
                text.styleClass.add("text")
            }

            tediView.document.linesProperty.addListener(this)
        }

        override fun layoutChildren() {
            println("TediContent.layoutChildren")
            val topPadding = snappedTopInset()
            val leftPadding = snappedLeftInset()

            var y = topPadding

            lines.forEach { line ->
                val bounds = line.boundsInLocal
                line.layoutX = leftPadding
                line.layoutY = y + bounds.height

                y += bounds.height

            }
        }

        override fun onChanged(c: ListChangeListener.Change<out String>) {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (i in 0..c.removedSize - 1) {
                        println("Removing line # ${c.from + i}")
                        lines.removeAt(c.from)
                        children.removeAt(c.from)
                    }
                } else if (c.wasAdded()) {
                    for (i in c.from..c.to - 1) {
                        val text = Text(tediView.document.lines[i])
                        lines.add(i, text)
                        children.add(i, text)
                    }
                }

            }

        }
    }

    init {
        scrollPane.content = content
        children.add(scrollPane)
    }

    private class CaretBlinking(blinkProperty: BooleanProperty) {

        val caretTimeline = Timeline()
        val blinkPropertyRef = WeakReference(blinkProperty)

        var blink: Boolean
            get() = blinkPropertyRef.get()?.get() == true
            set(v) {
                val property = blinkPropertyRef.get()
                if (property == null) {
                    caretTimeline.stop()
                } else {
                    property.set(v)
                }
            }

        init {

            caretTimeline.cycleCount = Timeline.INDEFINITE
            caretTimeline.keyFrames.addAll(
                    KeyFrame(Duration.ZERO, EventHandler { blink = false }),
                    KeyFrame(Duration.seconds(.5), EventHandler { blink = true }),
                    KeyFrame(Duration.seconds(1.0))
            )
        }

        fun start() {
            caretTimeline.play()
        }

        fun stop() {
            caretTimeline.stop()
        }

    }
}
