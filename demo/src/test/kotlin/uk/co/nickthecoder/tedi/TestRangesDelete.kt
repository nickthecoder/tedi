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
package uk.co.nickthecoder.tedi

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Delete portions of text, and check that the range is still correct.
 */
class TestRangesDelete {

    @JvmField @Rule
    val jfxRule = JavaFXThreadingRule()

    val highlight = StyleHighlight("-fx-fill: red;")

    lateinit var tediArea: TediArea

    @Before
    fun setUp() {
        tediArea = TediArea()
        tediArea.text = "abcdefghijk"
    }

    @After
    fun tearDown() {
    }

    fun rangeText(): String {
        val range = tediArea.highlightRanges()[0]
        return tediArea.getText(range.from, range.to)
    }

    @Test
    fun add() {
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        assertEquals(1, tediArea.highlightRanges().size, "Size 1")
        assertEquals(2, tediArea.highlightRanges()[0].from, "From")
        assertEquals(7, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun deleteInside() {
        // 01234567890
        // abcdefghijk
        //   -----
        // abcdfghijk
        //   ----
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        assertEquals(1, tediArea.highlightRanges().size, "Size 1")
        assertEquals("cdefg", rangeText(), "Range Before")
        tediArea.deleteText(4, 5)
        assertEquals("cdfg", rangeText(), "Range After")
        assertEquals(2, tediArea.highlightRanges()[0].from, "From")
        assertEquals(6, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun deleteBeyondFront() {
        // 01234567890
        // abcdefghijk
        //   -----
        // aefghijk
        //  ---
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        assertEquals(1, tediArea.highlightRanges().size, "Size 1")
        assertEquals("cdefg", rangeText(), "Range Before")
        tediArea.deleteText(1, 4)
        assertEquals("efg", rangeText(), "Range After")
        assertEquals(1, tediArea.highlightRanges()[0].from, "From")
        assertEquals(4, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun deleteBeyondFront2() {
        // 01234567890
        // abcdefghijk
        //    -----
        // afghijk
        //  ---
        tediArea.highlightRanges().add(HighlightRange(3, 8, highlight))
        assertEquals(1, tediArea.highlightRanges().size, "Size 1")
        assertEquals("defgh", rangeText(), "Range Before")
        tediArea.deleteText(1, 5)
        assertEquals("fgh", rangeText(), "Range After")
        assertEquals(1, tediArea.highlightRanges()[0].from, "From")
        assertEquals(4, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun deleteFront() {
        // 01234567890
        // abcdefghijk
        //   -----
        // abefghijk
        //   ---
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        assertEquals("cdefg", rangeText(), "Range Before")
        tediArea.deleteText(2, 4)
        assertEquals("efg", rangeText(), "Range After")
        assertEquals(2, tediArea.highlightRanges()[0].from, "From")
        assertEquals(5, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun deleteBack() {
        // 01234567890
        // abcdefghijk
        //   -----
        // abcdehijk
        //   ---
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        assertEquals("cdefg", rangeText(), "Range Before")
        tediArea.deleteText(5, 7)
        assertEquals("cde", rangeText(), "Range After")
        assertEquals(2, tediArea.highlightRanges()[0].from, "From")
        assertEquals(5, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun deleteBeyondBack() {
        // 01234567890
        // abcdefghijk
        //   -----
        // abcdejk
        //   ---
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        tediArea.deleteText(5, 9)
        assertEquals("cde", rangeText(), "Range After")
        assertEquals(2, tediArea.highlightRanges()[0].from, "From")
        assertEquals(5, tediArea.highlightRanges()[0].to, "To")
    }

    @Test
    fun deleteSame() {
        // 01234567890
        // abcdefghijk
        //   -----
        // abhijk
        //  ><
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        tediArea.deleteText(2, 7)
        assertEquals(0, tediArea.highlightRanges().size, "Size")
    }

    @Test
    fun deleteSameStretchy() {
        // 01234567890
        // abcdefghijk
        //   -----
        // abhijk
        //  ><
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight, stretchy = true))
        tediArea.deleteText(2, 7)
        assertEquals(1, tediArea.highlightRanges().size, "Size")
        assertEquals("", rangeText(), "Range After")
        assertEquals(2, tediArea.highlightRanges()[0].from, "From")
        assertEquals(2, tediArea.highlightRanges()[0].to, "To")

    }

    @Test
    fun deleteMoreBothWays() {
        // 01234567890
        // abcdefghijk
        //   -----
        // abhijk
        //  ><
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        tediArea.deleteText(1, 8)
        assertEquals(0, tediArea.highlightRanges().size, "Size")
    }

    @Test
    fun deleteMoreFront() {
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        tediArea.deleteText(1, 7)
        assertEquals(0, tediArea.highlightRanges().size, "Size")
    }

    @Test
    fun deleteMoreBack() {
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        tediArea.deleteText(2, 8)
        assertEquals(0, tediArea.highlightRanges().size, "Size")
    }

    @Test
    fun replaceHighlighted() {
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight))
        tediArea.replaceText(2, 7, "CDEFG")
        assertEquals(0, tediArea.highlightRanges().size, "Size")
    }

    @Test
    fun replaceHighlightedStretchy() {
        tediArea.highlightRanges().add(HighlightRange(2, 7, highlight, stretchy = true))
        tediArea.replaceText(2, 7, "CDEFG")
        assertEquals(1, tediArea.highlightRanges().size, "Size")
        assertEquals("CDEFG", rangeText(), "Range After")
        assertEquals(2, tediArea.highlightRanges()[0].from, "From")
        assertEquals(7, tediArea.highlightRanges()[0].to, "To")
    }

}
