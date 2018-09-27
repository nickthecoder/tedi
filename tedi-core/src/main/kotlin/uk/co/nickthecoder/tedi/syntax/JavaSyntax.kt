package uk.co.nickthecoder.tedi.syntax

/**
 * Creates HighlightRanges suitable for Kotlin source code.
 */
open class JavaSyntax()

    : RegexSyntax(listOf(
        RegexHighlight("keyword", KEYWORD_PATTERN),
        RegexHighlight("number", NUMBER_PATTERN),
        RegexHighlight("comment", COMMENT_PATTERN),
        RegexHighlight("annotation", ANNOTATION_PATTERN),
        RegexHighlight("paren", PAREN_PATTERN),
        RegexHighlight("brace", BRACE_PATTERN),
        RegexHighlight("bracket", BRACKET_PATTERN),
        RegexHighlight("string", STRING_PATTERN)
)) {

    companion object {

        /**
         * This list was taken from :
         * https://docs.oracle.com/javase/tutorial/java/nutsandbolts/_keywords.html
         */
        @JvmStatic
        val KEYWORDS = listOf(
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
                "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
                "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
                "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
                "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
        )

        @JvmStatic
        val KEYWORD_PATTERN = createKeywordsPattern(KEYWORDS)

        @JvmStatic
        val instance = JavaSyntax()
    }
}
