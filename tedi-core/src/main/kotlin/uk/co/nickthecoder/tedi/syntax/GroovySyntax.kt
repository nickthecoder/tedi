package uk.co.nickthecoder.tedi.syntax

/**
 * Creates HighlightRanges suitable for Groovy source code.
 */
open class GroovySyntax()
    : RegexSyntax(listOf(
        RegexHighlight("keyword", KEYWORD_PATTERN),
        RegexHighlight("number", NUMBER_PATTERN),
        RegexHighlight("comment", COMMENT_PATTERN),
        RegexHighlight("annotation", ANNOTATION_PATTERN),
        RegexHighlight("paren", PAREN_PATTERN),
        RegexHighlight("brace", BRACE_PATTERN),
        RegexHighlight("bracket", BRACKET_PATTERN),
        RegexHighlight("string", STRING_PATTERN),
        RegexHighlight("semicolon", SEMICOLON_EOL_PATTERN, ERROR_HIGHLIGHT)
)) {

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

        @JvmStatic
        val instance = GroovySyntax()
    }
}
