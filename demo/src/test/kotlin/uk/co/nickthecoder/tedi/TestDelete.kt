package uk.co.nickthecoder.tedi


import javafx.collections.ListChangeListener
import javafx.collections.ListChangeListener.Change
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.co.nickthecoder.tedi.ParagraphList.Paragraph
import kotlin.test.assertEquals

class TestDelete() : ListChangeListener<Paragraph> {

    @JvmField @Rule
    val jfxRule = JavaFXThreadingRule()

    lateinit var tediArea: TediArea

    var changes = mutableListOf<Change<out Paragraph>>()


    @Before
    fun setUp() {
        tediArea = TediArea()
        tediArea.paragraphsProperty().addListener(this)

        tediArea.text = ""
        changes.clear()
    }

    @After
    fun tearDown() {
        tediArea.paragraphsProperty().removeListener(this)
    }

    override fun onChanged(change: Change<out Paragraph>) {
        assertEquals(true, change.next(), "Change has Next")
        changes.add(change)
        assertEquals(false, change.next(), "Change has only one item")
    }

    @Test
    fun allOneLine() {
        tediArea.text = "Hello"
        changes.clear()
        tediArea.deleteText(0, 5)

        assertEquals("", tediArea.text, "Text")

        assertEquals(1, tediArea.paragraphs.size, "# Paragraphs")
        assertEquals("", tediArea.paragraphs[0].text, "Paragraph")

        assertEquals(1, changes.size, "# Changes")

        assertEquals(0, changes[0].from, "From")
        assertEquals(1, changes[0].to, "To")
        assertEquals(true, changes[0].wasUpdated(), "Updated")
        assertEquals(false, changes[0].wasAdded(), "Not Added")
        assertEquals(false, changes[0].wasRemoved(), "Not Removed")
    }

    @Test
    fun allTwoLines() {
        tediArea.text = "Hello\nWorld"
        changes.clear()
        tediArea.deleteText(0, 11)

        assertEquals("", tediArea.text, "Text")

        assertEquals(1, tediArea.paragraphs.size, "# Paragraphs")
        assertEquals("", tediArea.paragraphs[0].text, "Paragraph")

        assertEquals(2, changes.size, "# Changes")

        assertEquals(true, changes[0].wasUpdated(), "Updated")
        assertEquals(0, changes[0].from, "From")
        assertEquals(1, changes[0].to, "To")

        assertEquals(1, changes[1].from, "From")
        assertEquals(true, changes[1].wasRemoved(), "Removed")
        assertEquals(1, changes[1].removedSize, "# Removed")
    }

    @Test
    fun allThreeLines() {
        tediArea.text = "Hello\nWorld\nBye"
        changes.clear()
        tediArea.deleteText(0, 15)

        assertEquals("", tediArea.text, "Text")

        assertEquals(1, tediArea.paragraphs.size, "# Paragraphs")
        assertEquals("", tediArea.paragraphs[0].text, "Paragraph")

        assertEquals(2, changes.size, "# Changes")

        assertEquals(true, changes[0].wasUpdated(), "Updated")
        assertEquals(0, changes[0].from, "From")
        assertEquals(1, changes[0].to, "To")

        assertEquals(1, changes[1].from, "From")
        assertEquals(true, changes[1].wasRemoved(), "Removed")
        assertEquals(2, changes[1].removedSize, "# Removed")
    }


    @Test
    fun partOfOneLine() {
        tediArea.text = "Hello"
        changes.clear()
        tediArea.deleteText(1, 4)

        assertEquals("Ho", tediArea.text, "Text")

        assertEquals(1, tediArea.paragraphs.size, "# Paragraph")
        assertEquals("Ho", tediArea.paragraphs[0].text, "Paragraph")

        assertEquals(1, changes.size, "# Changes")

        assertEquals(0, changes[0].from, "From")
        assertEquals(1, changes[0].to, "To")
        assertEquals(true, changes[0].wasUpdated(), "Updated")
    }

    @Test
    fun partOfTwoLines() {
        tediArea.text = "Hello\nWorld"
        changes.clear()
        tediArea.deleteText(2, 9)

        assertEquals("Held", tediArea.text, "Text")

        assertEquals(1, tediArea.paragraphs.size, "# Paragraph")
        assertEquals("Held", tediArea.paragraphs[0].text, "Paragraph")

        assertEquals(2, changes.size, "# Changes")

        assertEquals(0, changes[0].from, "From")
        assertEquals(1, changes[0].to, "To")
        assertEquals(false, changes[0].wasRemoved(), "Not Removed")

        assertEquals(1, changes[1].from, "From")
        assertEquals(true, changes[1].wasRemoved(), "Removed")
        assertEquals(1, changes[1].removedSize, "# Removed")
    }


    @Test
    fun partOfTwoLines2() {
        tediArea.text = "Hello\nWorld"
        changes.clear()
        tediArea.deleteText(2, 11)

        assertEquals("He", tediArea.text, "Text")

        assertEquals(1, tediArea.paragraphs.size, "# Paragraph")
        assertEquals("He", tediArea.paragraphs[0].text, "Paragraph")

        assertEquals(2, changes.size, "# Changes")

        assertEquals(0, changes[0].from, "From")
        assertEquals(1, changes[0].to, "To")
        assertEquals(true, changes[0].wasUpdated(), "Updated")

        assertEquals(1, changes[1].from, "From")
        assertEquals(true, changes[1].wasRemoved(), "Removed")
        assertEquals(1, changes[1].removedSize, "# Removed")
    }

    @Test
    fun lastLine() {
        tediArea.text = "Hello\nWorld"
        changes.clear()
        tediArea.deleteText(5, 11)

        assertEquals("Hello", tediArea.text, "Text")

        assertEquals(1, tediArea.paragraphs.size, "# Paragraphs")
        assertEquals("Hello", tediArea.paragraphs[0].text, "Paragraph")

        assertEquals(1, changes.size, "# Changes")

        assertEquals(1, changes[0].from, "From")
        assertEquals(true, changes[0].wasRemoved(), "Removed")
        assertEquals(1, changes[0].removedSize, "# Removed")
    }

    /**
     * This one is a little weird. It sends an Update for the first line,
     * and then a delete for the 2nd.
     * That's because it takes the text from the 2nd and adds it to the 1st,
     * then deletes the 2nd.
     *
     * So, while it is weird, it is kind of ok.
     */
    @Test
    fun firstLine() {
        tediArea.text = "Hello\nWorld"
        changes.clear()
        tediArea.deleteText(0, 6)

        assertEquals("World", tediArea.text, "Text")

        assertEquals(1, tediArea.paragraphs.size, "# Paragraphs")
        assertEquals("World", tediArea.paragraphs[0].text, "Paragraph")

        //assertEquals(1, changes.size, "# Changes")

        //assertEquals(0, changes[0].from, "From")
        //assertEquals(true, changes[0].wasRemoved(), "Removed")
        //assertEquals(1, changes[0].removedSize, "# Removed")

        assertEquals(2, changes.size, "# Changes")

        assertEquals(0, changes[0].from, "From")
        assertEquals(1, changes[0].to, "To")
        assertEquals(true, changes[0].wasUpdated(), "Changed")

        assertEquals(1, changes[1].from, "From")
        assertEquals(true, changes[1].wasRemoved(), "Removed")
        assertEquals(1, changes[1].removedSize, "# Removed")
    }

    @Test
    fun middleLine() {
        tediArea.text = "Hello\nWorld\nBye"
        changes.clear()
        tediArea.deleteText(5, 11)

        assertEquals("Hello\nBye", tediArea.text, "Text")

        assertEquals(2, tediArea.paragraphs.size, "# Paragraphs")
        assertEquals("Hello", tediArea.paragraphs[0].text, "Paragraph")
        assertEquals("Bye", tediArea.paragraphs[1].text, "Paragraph")

        assertEquals(1, changes.size, "# Changes")

        assertEquals(1, changes[0].from, "From")
        assertEquals(true, changes[0].wasRemoved(), "Removed")
        assertEquals(1, changes[0].removedSize, "# Removed")
    }

}
