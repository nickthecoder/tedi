package uk.co.nickthecoder.tedi.syntax

import uk.co.nickthecoder.tedi.Highlight

/**
 * Creates HighlightRanges suitable for Kotlin source code.
 *
 * When using your own styles (i.e. choosing your own color scheme), the style map may contain
 * the following keys :
 *
 * - KEYWORD
 * - PAREN
 * - BRACE
 * - BRACKET
 * - SEMICOLON
 * - STRING
 * - COMMENT
 */
open class JavaSyntax(styles: Map<String, Highlight?>)

    : RegexSyntax(PATTERN, styles) {

    constructor() : this(defaultStyles)

    companion object {

        /**
         * This list was taken from :
         * https://docs.oracle.com/javase/tutorial/java/nutsandbolts/_keywords.html
         */
        @JvmStatic
        protected val KEYWORDS = listOf(
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
                "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
                "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
                "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
                "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
        )

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

        // Note the defaultStyles does NOT include "SEMICOLON", and therefore they will not create ranges.
        @JvmStatic
        val defaultStyles = hashMapOf(
                KEYWORD_STYLE, NUMBER_STYLE, ANNOTATION_STYLE, PAREN_STYLE, BRACE_STYLE, BRACKET_STYLE, STRING_STYLE, COMMENT_STYLE
        )

        @JvmStatic
        val instance = JavaSyntax()

    }

}
