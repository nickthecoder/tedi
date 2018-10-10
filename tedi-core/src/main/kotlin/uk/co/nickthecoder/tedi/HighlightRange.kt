package uk.co.nickthecoder.tedi

/**
 * Marks a portion of the document to be highlighted differently from the rest of the document.
 * When text is inserted/deleted, the range's from and to positions are updated accordingly.
 * All other parts of [HighlightRange] are immutable. So, for example, if you want to change the
 * [highlight], you must replace the range with a **new** one.
 *
 * Ranges can be used for syntax highlighting of source code, to highlight search matches and also
 * to style text in arbitrary ways.
 *
 * It is also used to highlight selected text.
 *
 * Add [HighlightRange]s to [TediArea.highlightRanges].
 *
 * [owner] is not used by TediArea, but can be used for whatever purpose you see fit.
 * The Syntax classes use it to filter TediArea.highlightRanges(), so
 * that ranges that it created can easily be removed, without affecting any other ranges
 * (such as those from find and replace).
 *
 * Note. Feel free to extend HighlightRange, for example, you could create a URLHighlightRange,
 * which has a URL.
 * Your application can use that to view the web page in a browser (maybe via a context menu, or
 * by double clicking on the highlighted range).
 *
 * [stretchy] ranges differ from non-stretchy ones when text is inserted at the front or end of the
 * range. When deleting text exactly the same size as a stretchy range will leave the range intact
 * (with zero length), however, a non-stretchy range will be deleted.
 *
 * TediArea pays no other attention to [stretchy], but this may be helpful to an application
 * which builds a rich-text around TediArea. For example, when inserting BOLD text, the range can be
 * initially stretchy (so that typing extends the range). Then when bold is turned off
 * (by pressing a button, or a keyboard shortcut), the range is changed to a non-stretchy one,
 * allowing plain text to be added.
 *
 * Note that [stretchy] is immutable, and therefore the highlight will need to be REPLACED, and
 * not merely changed.
 */
open class HighlightRange(
        internal var start: Int,
        internal var end: Int,
        val highlight: Highlight,
        val owner: Any? = null,
        val stretchy: Boolean = false)

    : Comparable<HighlightRange> {

    constructor(start: Int, end: Int, highlight: Highlight) : this(start, end, highlight, null)

    val from
        get() = start

    val to
        get() = end

    /**
     * There is a bi-directional relationship between [ParagraphList.Paragraph] and [HighlightRange].
     * TediArea maintains this relationship when inserting/deleting text.
     * The relationship exists to optimise the conversion from a [ParagraphList.Paragraph] to
     * its corresponding visual Text representation in the Skin.
     */
    internal val affectedParagraphs = mutableSetOf<ParagraphList.Paragraph>()

    /**
     * NOTE. This is NOT consistent with equals. (See [Comparable] for details of why this matters).
     *
     * If two ranges have the same start and end positions, but differ in other ways, then
     * compareTo will return 0, but equals will return false.
     *
     * TLDR; Beware, when using in sorted sets, and sorted maps.
     */
    override fun compareTo(other: HighlightRange): Int {
        if (start == other.start) return end - other.end
        return start - other.start
    }

    override fun toString() = "$start..$end : $highlight"
}


/**
 * A highlight range, which is part of a matched pair, for example, an opening and closing bracket.
 *
 * Always create [PairedHighlightRange] in pairs, never alone, or with more than two items.
 *
 * Note, TediArea knows nothing of PairedHighlightRanges, and will happily remove one of them from its
 * list, leaving the other untouched.
 */
class PairedHighlightRange

/**
 * @param opening Set to null for the 1st ot the pair to be created.
 * When creating the 2nd of the pair (with opening != null), the first range will be suitably updated.
 */
(start: Int, end: Int, highlight: Highlight, opening: PairedHighlightRange?) : HighlightRange(start, end, highlight) {

    /*
     * Note, because of a chicken and egg problem, we cannot have a simple 'val' for [other].
     * Instead, opening = null for the first of the pair, and then the second instance
     * will assign BOTH of the [other]s in init.
     */
    private lateinit var other: PairedHighlightRange


    val pairedWith: PairedHighlightRange
        get() = other

    init {
        if (opening != null) {
            other = opening
            opening.other = this
        }
    }

}
