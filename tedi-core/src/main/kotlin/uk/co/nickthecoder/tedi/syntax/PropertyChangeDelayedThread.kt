package uk.co.nickthecoder.tedi.syntax

import javafx.beans.property.Property
import javafx.beans.value.ChangeListener
import java.util.concurrent.CountDownLatch

/**
 * Whenever the [property] changes, start a new Thread, and wait for [delay] milliseconds,
 * then run the [action].
 *
 * If another change to [property] occurs before the delay is complete, then the thread is interrupted
 * and the process repeats, waiting for delay.
 *
 * I use this to perform syntax highlighting from within the Demo application.
 *
 * NOTE. I'm not great with multi-threading code, and I don't know how to test this well.
 *
 * @return The ChangeListener. call [property].removeListener( resultValue ) to stop listening to change events.
 */
fun <T> propertyChangeDelayedThread(property: Property<T>, delay: Long, action: () -> Unit): ChangeListener<T> {

    var thread: Thread? = null
    var actionLatch: CountDownLatch? = null

    val listener = ChangeListener<T> { _, _, _ ->

        thread?.interrupt() // Interrupt the thread if there is one.

        val newThread = Thread {
            try {
                actionLatch?.await() // Wait for the previous action to finish if there is one.
                Thread.sleep(delay)

                // If the thread is interrupted during sleep, then we won't get to here.
                // So no further action will be performed.

                actionLatch = CountDownLatch(1)
                try {
                    action()
                } finally {
                    thread = null
                    actionLatch?.countDown()
                }
            } catch (e : InterruptedException) {
                // Do nothing.
            }
        }
        newThread.start()
        thread = newThread

    }
    property.addListener(listener)
    return listener
}