package uk.co.nickthecoder.tedi

/**
 * Used internally by TediArea to help spilt Paragraphs into pieces, where each piece is
 * highlighted in a different manner.
 * The start and end of the [HighlightRange] are ignored after the HighlightSlice is created,
 * and are only kept so that when [HighlightRange]s are removed, it is easy to tell which
 * slices can be removed or merged.
 */
internal class ParagraphHighlightRange(
        var startColumn: Int,
        var endColumn: Int,
        val cause: HighlightRange) {

    fun intersects(from: Int, to: Int): Boolean = from >= startColumn && to <= endColumn

    override fun toString() = "PHR $startColumn..$endColumn from $cause"
}
