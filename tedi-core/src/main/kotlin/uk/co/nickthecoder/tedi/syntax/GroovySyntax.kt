package uk.co.nickthecoder.tedi.syntax

/**
 * Creates HighlightRanges suitable for Groovy source code.
 */
open class GroovySyntax()
    : RegexSyntax(listOf(
        KEYWORD, NUMBER, C_COMMENT, ANNOTATION, STRING,
        OPEN_PAREN, CLOSE_PAREN, OPEN_BRACE, CLOSE_BRACE, OPEN_BRACKET, CLOSE_BRACKET,
        WASTEFUL_SEMICOLON)) {

    companion object {

        /**
         * Taken from :
         * http://docs.groovy-lang.org/latest/html/documentation/
         */
        @JvmStatic
        val KEYWORDS = listOf(
                "as", "assert", "break", "case", "catch", "class", "const", "continue",
                "def", "default", "do", "else", "enum", "extends", "false", "finally", "for", "goto", "if",
                "implements", "import", "in", "instanceof", "interface", "new", "null", "package", "return",
                "super", "switch", "this", "throw", "throws", "trait", "true", "try", "while")


        @JvmStatic
        val KEYWORD_PATTERN = createKeywordsPattern(KEYWORDS)

        val KEYWORD = RegexHighlight("keyword", JavaSyntax.KEYWORD_PATTERN)

        @JvmStatic
        val instance = GroovySyntax()
    }
}
