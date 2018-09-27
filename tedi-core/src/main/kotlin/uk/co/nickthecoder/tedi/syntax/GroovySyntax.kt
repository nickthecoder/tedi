package uk.co.nickthecoder.tedi.syntax

import uk.co.nickthecoder.tedi.Highlight

/**
 * Creates HighlightRanges suitable for Kotlin source code.
 *
 * When using your own styles (i.e. choosing your own color scheme), the style map may contain
 * the following keys :
 *
 * - KEYWORD
 * - ANNOTATION
 * - NUMBER
 * - PAREN
 * - BRACE
 * - BRACKET
 * - SEMICOLON
 * - STRING
 * - COMMENT
 */
open class GroovySyntax(styles: Map<String, Highlight>) : RegexSyntax(PATTERN, styles) {

    constructor() : this(JavaSyntax.defaultStyles)

    companion object {

        /**
         * Taken from :
         * http://docs.groovy-lang.org/latest/html/documentation/
         */
        @JvmStatic
        protected val KEYWORDS = listOf(
                "as", "assert", "break", "case", "catch", "class", "const", "continue",
                "def", "default", "do", "else", "enum", "extends", "false", "finally", "for", "goto", "if",
                "implements", "import", "in", "instanceof", "interface", "new", "null", "package", "return",
                "super", "switch", "this", "throw", "throws", "trait", "true", "try", "while")


        @JvmStatic
        protected val KEYWORD_PATTERN = createKeywordsPattern(KEYWORDS)

        @JvmStatic
        protected val PATTERN = buildPattern(mapOf(
                "KEYWORD" to KEYWORD_PATTERN,
                "NUMBER" to NUMBER_PATTERN,
                "ANNOTATION" to ANNOTATION_PATTERN,
                "PAREN" to PAREN_PATTERN,
                "BRACE" to BRACE_PATTERN,
                "BRACKET" to BRACKET_PATTERN,
                "SEMICOLON" to SEMICOLON_PATTERN,
                "STRING" to STRING_PATTERN,
                "COMMENT" to COMMENT_PATTERN))

    }
}
