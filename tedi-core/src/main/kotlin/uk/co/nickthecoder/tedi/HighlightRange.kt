package uk.co.nickthecoder.tedi

/**
 * Marks a portion of the document to be highlighted differently from the rest of the document.
 *
 * This can be used for syntax highlighting for source code, and also to highlight search matches.
 *
 * It is NOT used for highlighting the currently selected area.
 *
 * Add [HighlightRange]s to [TediArea.highlightRanges].
 */
class HighlightRange(
        val from: Int,
        val to: Int,
        val highlight: Highlight) {

    internal val affectedParagraphs = mutableSetOf<ParagraphList.Paragraph>()
}

