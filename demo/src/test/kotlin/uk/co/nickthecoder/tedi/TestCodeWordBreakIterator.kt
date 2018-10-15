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
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import uk.co.nickthecoder.tedi.SourceCodeWordIterator
import java.text.BreakIterator
import java.text.StringCharacterIterator

class TestCodeWordBreakIterator {

    /**
     * This tests the "default" word break iterator.
     * I used this, just so that I understood how the existing one worked.
     * However, maybe it should be removed from the test suite (remove the @Test),
     * because someone with a different locale will get a different BreakIterator,
     * which may act differently.
     */
    @Test
    fun defaultWordBreakIterator() {
        val bi = BreakIterator.getWordInstance()
        check(bi, "Hello world have a nice day", 5, 6, 11, 12, 16, 17, 18, 19, 23, 24, 27)
        check(bi, "Hello, world have a nice day", 5, 6, 7, 12, 13, 17, 18, 19, 20, 24, 25, 28)

        // These are horrible, and why I need to create my own!
        check(bi, "uk.co.nickthecoder", 18)
        check(bi, "package uk.co.nickthecoder", 7, 8, 26) // uk.co.nickthecoder is one word
        check(bi, "a += 2", 1, 2, 3, 4, 5, 6) // += is TWO words.
    }

    /**
     * The same tests as [defaultWordBreakIterator], but using my word iterator.
     * Notice how
     */
    @Test
    fun myWordBreakIterator() {
        check("Hello world have a nice day", 5, 6, 11, 12, 16, 17, 18, 19, 23, 24, 27)
        check("Hello, world have a nice day", 5, 6, 7, 12, 13, 17, 18, 19, 20, 24, 25, 28)

        // These are better now!
        check("uk.co.nickthecoder", 2, 3, 5, 6, 18)
        check("package uk.co.nickthecoder", 7, 8, 10, 11, 13, 14, 26) // uk.co.nickthecoder is NOT one word
        check("a += 2", 1, 2, 4, 5, 6) // += are one word
    }

    @Test
    fun testSimpleWords() {
        check("Hello world have a nice day", 5, 6, 11, 12, 16, 17, 18, 19, 23, 24, 27)
    }

    @Test
    fun testWordsAndSymbols() {
        check("Hello, world have a nice day", 5, 6, 7, 12, 13, 17, 18, 19, 20, 24, 25, 28)
        check("uk.co.nickthecoder", 2, 3, 5, 6, 18)
        check("package uk.co.nickthecoder.tedi", 7, 8, 10, 11, 13, 14, 26, 27, 31)
    }

    @Test
    fun testWordsAndMultipleSymbols() {
        check("a+=1", 1, 3, 4)
        check("a += 1", 1, 2, 4, 5, 6)
    }

    @Test
    fun lineBreaks() {
        check("abc\ndefgh\nijk", 3, 4, 9, 10, 13)
    }

    /**
     * Note, this is the same as [lineBreaks], but with symbols for the 2nd line.
     */
    @Test
    fun nonWords() {
        check("abc\n-----\ndef", 3, 4, 9, 10, 13)
    }

    fun check(sentence: String, vararg breaks: Int) {
        check(SourceCodeWordIterator(), sentence, * breaks)
    }

    fun check(bi: BreakIterator, sentence: String, vararg breaks: Int) {

        bi.text = StringCharacterIterator(sentence)
        var i = 0
        var b = bi.next()
        while (b >= 0) {
            if (i >= breaks.size) {
                fail("Not enough breaks (i=$i) for : '$sentence'")
            }
            assertEquals("$sentence break #$i", breaks[i], b)
            i++
            b = bi.next()
        }
        assertEquals("Too many breaks for '$sentence' : ", breaks.size, i)
    }
}
