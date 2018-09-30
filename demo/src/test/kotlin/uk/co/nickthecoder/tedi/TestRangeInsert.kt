package uk.co.nickthecoder.tedi

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals


/**
 * Inserts text, and check that the range is still correct.
 */
class TestRangesInsert {

    @JvmField @Rule
    val jfxRule = JavaFXThreadingRule()

    val highlight = StyleHighlight("-fx-fill: red;")

    lateinit var tediArea: TediArea

    @Before
    fun setUp() {
        tediArea = TediArea()
        tediArea.text = "abcdefghijk"
        tediArea.highlightRanges().clear()
    }

    @After
    fun tearDown() {
    }

    fun rangeText(): String {
        val range = tediArea.highlightRanges()[0]
        return tediArea.getText(range.from, range.to)
    }

    @Test
    fun inMiddle() {
        // 01234567890
        // abcdefghijk
        //   -----
        // abcdXefghijk
        //   ------
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        tediArea.insertText(4, "X")
        assertEquals("cdXefg", rangeText(), "Range After")
        assertEquals(2, tediArea.highlightRanges()[0].from, "From")
        assertEquals(8, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun before() {
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        tediArea.insertText(1, "X")
        assertEquals("cdefg", rangeText(), "Range After")
        assertEquals(3, tediArea.highlightRanges()[0].from, "From")
        assertEquals(8, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun after() {
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        tediArea.insertText(8, "X")
        assertEquals("cdefg", rangeText(), "Range After")
        assertEquals(2, tediArea.highlightRanges()[0].from, "From")
        assertEquals(7, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun atFront() {
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        tediArea.insertText(2, "X")
        assertEquals("cdefg", rangeText(), "Range After")
        assertEquals(3, tediArea.highlightRanges()[0].from, "From")
        assertEquals(8, tediArea.highlightRanges()[0].to, "To")
    }


    // Like [atFront], but with a stretchy highlight.
    @Test
    fun atFrontStretchy() { // Should NOT be included in the range.
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight, stretchy = true))
        tediArea.insertText(2, "X")
        assertEquals("Xcdefg", rangeText(), "Range After")
        assertEquals(2, tediArea.highlightRanges()[0].from, "From")
        assertEquals(8, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun atEnd() {
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        tediArea.insertText(7, "X")
        assertEquals("cdefg", rangeText(), "Range After")
        assertEquals(2, tediArea.highlightRanges()[0].from, "From")
        assertEquals(7, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun atEndStretchy() {
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight, stretchy = true))
        tediArea.insertText(7, "X")
        assertEquals("cdefgX", rangeText(), "Range After")
        assertEquals(2, tediArea.highlightRanges()[0].from, "From")
        assertEquals(8, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun atZeroLengthRange() {
        tediArea.highlightRanges().add(HighlightRange(2, 2, highlight))
        tediArea.insertText(2, "X")
        assertEquals("", rangeText(), "Range After")
        assertEquals(3, tediArea.highlightRanges()[0].from, "From")
        assertEquals(3, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun atZeroLengthRangeStretchy() {
        tediArea.highlightRanges().add(HighlightRange(2, 2, highlight, stretchy = true))
        tediArea.insertText(2, "X")
        assertEquals("X", rangeText(), "Range After")
        assertEquals(2, tediArea.highlightRanges()[0].from, "From")
        assertEquals(3, tediArea.highlightRanges()[0].to, "To")
    }

}
