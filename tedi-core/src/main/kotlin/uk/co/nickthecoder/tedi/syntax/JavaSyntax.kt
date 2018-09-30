package uk.co.nickthecoder.tedi.syntax

/**
 * Creates HighlightRanges suitable for Kotlin source code.
 */
open class JavaSyntax()

    : RegexSyntax(listOf(
        KEYWORD, NUMBER, C_COMMENT, ANNOTATION, STRING,
        OPEN_PAREN, CLOSE_PAREN, OPEN_BRACE, CLOSE_BRACE, OPEN_BRACKET, CLOSE_BRACKET,
        SEMICOLON)) {

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

        val KEYWORD = RegexHighlight("keyword", KEYWORD_PATTERN)

        @JvmStatic
        val instance = JavaSyntax()
    }
}
