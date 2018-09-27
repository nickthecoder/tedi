package uk.co.nickthecoder.tedi

/**
 * Marks a portion of the document to be highlighted differently from the rest of the document.
 *
 * This can be used for syntax highlighting for source code, and also to highlight search matches.
 *
 * It is NOT used for highlighting the currently selected area.
 *
 * Add [HighlightRange]s to [TediArea.highlightRanges].
 *
 * [owner] is not used by TediArea, but can be used for whatever purpose you see fit.
 * The Syntax class use it to filter TediArea.highlightRanges(), so
 * that ranges that it created can easily be removed, without affecting any other ranges
 * (such as those from find and replace).
 *
 * Note. Feel free to extend HighlightRange, for example, you could create a URLHighlightRange,
 * which has a URL.
 * Your application can use that to view the web page in a browser (maybe via a context menu, or
 * by double clicking on the highlighted range).
 */
open class HighlightRange(
        internal var start: Int,
        internal var end: Int,
        val highlight: Highlight,
        val owner: Any?) {

    constructor(start: Int, end: Int, highlight: Highlight) : this(start, end, highlight, null)

    val from
        get() = start

    val to
        get() = end

    internal val affectedParagraphs = mutableSetOf<ParagraphList.Paragraph>()

    fun contains(position: Int) = start >= position && end <= position

    override fun toString() = "$start..$end : $highlight"
}
