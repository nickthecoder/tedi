package uk.co.nickthecoder.tedi

import javafx.collections.ListChangeListener
import javafx.collections.ListChangeListener.Change
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.co.nickthecoder.tedi.ParagraphList.Paragraph
import kotlin.test.assertEquals

class TestInsert : ListChangeListener<Paragraph> {

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
    fun doNothing() {
        assertEquals("", tediArea.text, "Text")

        assertEquals(1, tediArea.paragraphs.size, "# Paragraphs")
        assertEquals("", tediArea.paragraphs[0].text, "Paragraph")

        assertEquals(0, changes.size, "# Changes")
    }

    @Test
    fun hello() {
        tediArea.insertText(0, "Hello")

        assertEquals("Hello", tediArea.text, "Text")

        assertEquals(1, tediArea.paragraphs.size, "# Paragraphs")
        assertEquals("Hello", tediArea.paragraphs[0].text, "Paragraph")

        assertEquals(1, changes.size, "# Changes")

        assertEquals(0, changes[0].from, "From")
        assertEquals(1, changes[0].to, "To")
        assertEquals(false, changes[0].wasRemoved(), "Not Removed")
        assertEquals(false, changes[0].wasAdded(), "Not Added")
        assertEquals(true, changes[0].wasUpdated(), "Updated")
    }

    @Test
    fun he_ll_o() {
        tediArea.insertText(0, "Heo")
        changes.clear()
        tediArea.insertText(2, "ll")

        assertEquals("Hello", tediArea.text, "Text")

        assertEquals(1, tediArea.paragraphs.size, "# Paragraphs")
        assertEquals("Hello", tediArea.paragraphs[0].text, "Paragraph")

        assertEquals(1, changes.size, "# Changes")

        assertEquals(0, changes[0].from, "From")
        assertEquals(1, changes[0].to, "To")
        assertEquals(false, changes[0].wasAdded(), "Not Added")
        assertEquals(true, changes[0].wasUpdated(), "Updated")
    }

    /**
     * Insert 2 lines into a blank document
     */
    @Test
    fun helloWorld() {
        tediArea.insertText(0, "Hello\nWorld")

        assertEquals("Hello\nWorld", tediArea.text, "Text")

        assertEquals(2, tediArea.paragraphs.size, "# Paragraphs")
        assertEquals("Hello", tediArea.paragraphs[0].text, "Paragraph")
        assertEquals("World", tediArea.paragraphs[1].text, "Paragraph")

        assertEquals(2, changes.size, "# Changes")

        assertEquals(true, changes[0].wasUpdated(), "Updated")
        assertEquals(0, changes[0].from, "From")
        assertEquals(1, changes[0].to, "To")

        assertEquals(true, changes[1].wasAdded(), "Added")
        assertEquals(1, changes[1].from, "From")
        assertEquals(2, changes[1].to, "To")
    }

    /**
     * Insert 2 lines into the middle of a line
     */
    @Test
    fun he_lloWor_ld() {
        tediArea.insertText(0, "Held")
        changes.clear()
        tediArea.insertText(2, "llo\nWor")

        assertEquals("Hello\nWorld", tediArea.text, "Text")

        assertEquals(2, tediArea.paragraphs.size, "# Paragraphs")
        assertEquals("Hello", tediArea.paragraphs[0].text, "Paragraph")
        assertEquals("World", tediArea.paragraphs[1].text, "Paragraph")

        assertEquals(3, changes.size, "# Changes")

        assertEquals(true, changes[0].wasUpdated(), "Updated")
        assertEquals(0, changes[0].from, "From")
        assertEquals(1, changes[0].to, "To")

        assertEquals(true, changes[1].wasAdded(), "Added")
        assertEquals(1, changes[1].from, "From")
        assertEquals(2, changes[1].to, "To")

        assertEquals(true, changes[2].wasUpdated(), "Updated")
        assertEquals(1, changes[2].from, "From")
        assertEquals(2, changes[2].to, "To")
    }

    /**
     * Insert three lines into an empty document
     */
    @Test
    fun helloWorldBye() {
        tediArea.insertText(0, "Hello\nWorld\nBye")

        assertEquals("Hello\nWorld\nBye", tediArea.text, "Text")

        assertEquals(3, tediArea.paragraphs.size, "# Paragraphs")
        assertEquals("Hello", tediArea.paragraphs[0].text, "Paragraph")
        assertEquals("World", tediArea.paragraphs[1].text, "Paragraph")
        assertEquals("Bye", tediArea.paragraphs[2].text, "Paragraph")

        assertEquals(2, changes.size, "# Changes")

        assertEquals(true, changes[0].wasUpdated(), "Updated")
        assertEquals(0, changes[0].from, "From")
        assertEquals(1, changes[0].to, "To")

        assertEquals(true, changes[1].wasAdded(), "Added")
        assertEquals(1, changes[1].from, "From")
        assertEquals(3, changes[1].to, "To")
    }

    /**
     * Insert three lines into the middle of a line
     */
    @Test
    fun he_lloWorldBy_e() {
        tediArea.insertText(0, "Hee")
        changes.clear()
        tediArea.insertText(2, "llo\nWorld\nBy")

        assertEquals("Hello\nWorld\nBye", tediArea.text, "Text")

        assertEquals(3, tediArea.paragraphs.size, "# Paragraphs")
        assertEquals("Hello", tediArea.paragraphs[0].text, "Paragraph")
        assertEquals("World", tediArea.paragraphs[1].text, "Paragraph")
        assertEquals("Bye", tediArea.paragraphs[2].text, "Paragraph")

        assertEquals(3, changes.size, "# Changes")

        assertEquals(true, changes[0].wasUpdated(), "Updated")
        assertEquals(0, changes[0].from, "From")
        assertEquals(1, changes[0].to, "To")

        assertEquals(true, changes[1].wasAdded(), "Added")
        assertEquals(1, changes[1].from, "From")
        assertEquals(3, changes[1].to, "To")

        assertEquals(true, changes[2].wasUpdated(), "Updated")
        assertEquals(2, changes[2].from, "From")
        assertEquals(3, changes[2].to, "To")
    }

}
