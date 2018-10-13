package uk.co.nickthecoder.tedi.util

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import java.util.concurrent.CountDownLatch

/**
 * Whenever the [property] changes, start a new Thread, and [wait] for a while (in milliseconds),
 * then run the [action].
 *
 * If another change to [property] occurs before the wait is complete, then the thread is interrupted
 * and the process repeats, waiting again.
 *
 * I use this to perform syntax highlighting.
 *
 * NOTE. I'm not great with multi-threading code, and I don't know how to test this well.
 *
 * @return The ChangeListener. call [property].removeListener( resultValue ) to stop listening to change events.
 */
fun <T> propertyChangeDelayedThread(property: ObservableValue<T>, wait: Long, action: (T) -> Unit): ChangeListener<T> {

    var thread: Thread? = null
    var actionLatch: CountDownLatch? = null

    val listener = ChangeListener<T> { _, _, newValue: T ->

        thread?.interrupt() // Interrupt the thread if there is one.

        val newThread = Thread {
            try {
                actionLatch?.await() // Wait for the previous action to finish if there is one.
                Thread.sleep(wait)

                // If the thread is interrupted during sleep, then we won't get to here.
                // So no further action will be performed.

                actionLatch = CountDownLatch(1)
                try {
                    action(newValue)
                } finally {
                    thread = null
                    actionLatch?.countDown()
                }
            } catch (e: InterruptedException) {
                // Do nothing.
            }
        }
        newThread.start()
        thread = newThread

    }
    property.addListener(listener)
    return listener
}
