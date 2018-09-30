package uk.co.nickthecoder.tedi

import javafx.collections.ListChangeListener
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Tests that Change events are fired when items are added/removed from [TediArea.highlightRanges].
 */
class TestRangeEvents : ListChangeListener<HighlightRange> {

    @JvmField @Rule
    val jfxRule = JavaFXThreadingRule()

    val changes = mutableListOf<ListChangeListener.Change<out HighlightRange>>()

    val highlight = StyleClassHighlight("foo")

    lateinit var tediArea: TediArea

    @Before
    fun setUp() {
        tediArea = TediArea()
        tediArea.highlightRanges().addListener(this)

        tediArea.text = "012345678901234567890"
        changes.clear()
    }

    @After
    fun tearDown() {
        tediArea.highlightRanges().removeListener(this)
        changes.clear()
    }


    override fun onChanged(change: ListChangeListener.Change<out HighlightRange>) {
        assertEquals(true, change.next(), "Change has Next")
        changes.add(change)
        assertEquals(false, change.next(), "Change has only one item")
    }

    @Test
    fun simpleAdd() {
        tediArea.highlightRanges().add(HighlightRange(2, 4, highlight))
        assertEquals(1, changes.size, "Size")
        assertEquals(true, changes[0].wasAdded(), "Was Added")
    }

    @Test
    fun simpleRemove() {
        val range = HighlightRange(2, 4, highlight)
        tediArea.highlightRanges().add(range)
        tediArea.highlightRanges().remove(range)
        assertEquals(2, changes.size, "Size")
        assertEquals(true, changes[0].wasAdded(), "Was Added")
        assertEquals(true, changes[1].wasRemoved(), "Was Removed")
    }

    @Test
    fun deleteText() {
        val range = HighlightRange(2, 4, highlight)
        tediArea.highlightRanges().add(range)
        tediArea.deleteText(2,4)
        assertEquals(2, changes.size, "Size")
        assertEquals(true, changes[0].wasAdded(), "Was Added")
        assertEquals(true, changes[1].wasRemoved(), "Was Removed")
    }



}
