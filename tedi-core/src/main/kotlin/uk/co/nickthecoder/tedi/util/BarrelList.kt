package uk.co.nickthecoder.tedi.util

import java.util.*

private val INITIAL_SIZE = 50
private val INCREMENT_SIZE = 10

/**
 * A class specially designed to be efficient for the type of list required by VirtualView's
 * cells list.
 * The size of the list remains fairly constant (it will be the number of visible cells in the viewport),
 * so an array seems in order.
 *
 * We frequently need to add items to either the front of the list, or the end.
 * A traditional array is expensive when adding to the front (as all data must shift along one).
 * A linked list is expensive pretty much always.
 *
 * The solution is a barrel list. We pre-allocate an array, and then keep track of where the "real"
 * start and end points are. Adding to the end only requires the lastIndex to be incremented,
 * Adding to the front only requires decrementing the firstIndex (and looping back around in both
 * cases when needed).
 *
 * We also need to grow the barrel, when there is no more room left (and then we DO need to shovel
 * data around).
 *
 * PS. While this makes VirtualView's cells list efficient, the nodeGroup's children cannot be
 * optimised (you aren't allowed to change getChildren() to be a different type of list).
 * And that collection changes just as frequently as "cells", so we aren't changing the overall
 * order of complexity, O(), we're just dividing it by 2 :-(
 *
 * Note, nodeGroup.getChildren() doesn't have to be ordered, so I *could* implement deletes from
 * the middle, by moving, and then deleting from the end. However, this is probably WORSE, because
 * removing a Node, and replacing it is more expensive (e.g. the css for that node would need to be
 * re-evaluated I assume).
 */
class BarrelList<T> : AbstractList<T>() {

    private val array = ArrayList<T?>(INITIAL_SIZE)

    private var firstIndex = -1
    private var lastIndex = -1
    private var theSize = 0

    init {
        repeat(INITIAL_SIZE) {
            array.add(null)
        }
    }

    val first: T
        get() = array[firstIndex]!!

    val last: T
        get() = array[lastIndex]!!

    val firstOrNull: T?
        get() = if (firstIndex == -1) null else array[firstIndex]

    val lastOrNull: T?
        get() = if (lastIndex == -1) null else array[lastIndex]


    private fun grow() {
        if (array.size != theSize) throw IllegalStateException("The barrel isn't full")

        val oldSize = array.size
        array.ensureCapacity(array.size + INCREMENT_SIZE)
        repeat(INCREMENT_SIZE) {
            array.add(null)
        }

        if (firstIndex == 0) {
            // START first...last END
            // Wow, lucky, we don't need to do anything more!
        } else {
            // START...last first...END
            // Move everything UP, and adjust firstIndex
            for (i in oldSize - 1 downTo firstIndex) {
                array[i + INCREMENT_SIZE] = array[i]
            }
            for (i in 0..INCREMENT_SIZE - 1) {
                array[firstIndex + i] = null
            }
            firstIndex += INCREMENT_SIZE
            // NOTE, if firstIndex was near the beginning, then we could have made the gap at the beginning,
            // rather than the end, and thus saved some time.
        }
    }

    fun addFirst(cell: T) {
        // if firstIndex == -1 then that means this is the first item in the
        // list and we need to initialize firstIndex and lastIndex
        if (firstIndex == -1) {
            firstIndex = 0
            lastIndex = 0
        } else {
            if (array.size <= theSize) {
                grow()
            }
            if (firstIndex == 0) {
                firstIndex = array.size - 1
            } else {
                firstIndex--
            }
        }
        array[firstIndex] = cell
        theSize++
    }

    fun addLast(cell: T) {
        if (lastIndex == -1) {
            firstIndex = 0
            lastIndex = 0
        } else {
            if (array.size <= theSize) {
                grow()
            }
            if (lastIndex == array.size - 1) {
                lastIndex = 0
            } else {
                lastIndex++
            }
        }
        array[lastIndex] = cell
        theSize++
    }

    override val size: Int
        get() = theSize

    override fun isEmpty(): Boolean {
        return theSize == 0
    }

    override fun get(index: Int): T {
        if (index >= array.size || index < 0) throw ArrayIndexOutOfBoundsException(index)
        val i = firstIndex + index
        if (i >= array.size) {
            return array[i - array.size]!!
        } else {
            return array[i]!!
        }
    }

    override fun clear() {
        if (theSize == 0) return
        if (firstIndex <= lastIndex) {
            for (i in firstIndex..lastIndex) {
                array[i] = null
            }
        } else {
            for (i in firstIndex..array.size - 1) {
                array[i] = null
            }
            for (i in 0..lastIndex) {
                array[i] = null
            }
        }
        theSize = 0
        firstIndex = -1
        lastIndex = -1
    }

    fun removeFirst(): T {
        if (theSize == 0) throw IndexOutOfBoundsException()
        val result = array[firstIndex]
        array[firstIndex] = null

        firstIndex++
        if (firstIndex == array.size) firstIndex = 0

        theSize--
        return result!!
    }

    fun removeLast(): T {
        if (theSize == 0) throw IndexOutOfBoundsException()
        val result = array[lastIndex]
        array[lastIndex] = null

        lastIndex--
        if (lastIndex == -1) lastIndex = array.size - 1

        theSize--
        return result!!
    }

    override fun removeAt(index: Int): T {
        if (index >= array.size || index < 0) throw ArrayIndexOutOfBoundsException(index)

        theSize--

        var arrayIndex: Int = firstIndex + index
        if (arrayIndex >= array.size) arrayIndex -= array.size
        val result = array[arrayIndex]
        array[arrayIndex] = null

        if (arrayIndex == firstIndex) {
            // Remove from the front
            firstIndex++
            if (firstIndex == array.size) firstIndex = 0
        } else if (arrayIndex == lastIndex) {
            // Remove from the end
            lastIndex--
            if (lastIndex == -1) lastIndex = array.size - 1
        } else {
            // TODO Test me!
            // Remove from the middle.

            // We have 3 different scenarios :
            // 1) <gap> firstIndex...arrayIndex...lastIndex <gap>
            // 2) START...arrayIndex...lastIndex <gap> firstIndex...END
            // 3) START...lastIndex <gap> firstIndex...arrayIndex...END

            // where <gap> may be zero or more nulls, and ... is zero or more non-nulls.

            // For (1) and (2), we just need to move data down one place from arrayIndex + 2 to lastIndex
            // But for (3), we need two loops. From arrayIndex + 2 to array.size-1, then from 0 to lastIndex
            // with special care when crossing the boundary.
            if (lastIndex >= arrayIndex) {
                // (1) and (2)
                for (i in arrayIndex + 2..lastIndex) {
                    array[i - 1] = array[i]
                }
            } else {
                // (3)
                for (i in arrayIndex + 2..array.size - 1) {
                    array[i - 1] = array[i]
                }
                array[array.size - 1] = array[0]
                for (i in 2..lastIndex) {
                    array[i - 1] = array[i]
                }
            }
            lastIndex--
            if (lastIndex == -1) {
                lastIndex = array.size - 1
            }
        }

        return result!!
    }
}

