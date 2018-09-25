package uk.co.nickthecoder.tedi

/**
 * Marks a portion of the document so that the
 */
class HighlightRange(
        val from: Int,
        val to: Int,
        val highlight: Highlight) {

    internal val affectedParagraphs = mutableSetOf<ParagraphList.Paragraph>()
}

