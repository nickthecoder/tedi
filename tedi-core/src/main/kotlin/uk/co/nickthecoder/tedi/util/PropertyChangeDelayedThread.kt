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
