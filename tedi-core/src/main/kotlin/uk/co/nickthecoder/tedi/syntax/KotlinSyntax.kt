package uk.co.nickthecoder.tedi.syntax

import uk.co.nickthecoder.tedi.Highlight
import uk.co.nickthecoder.tedi.StyleHighlight

/**
 * Creates HighlightRanges suitable for Kotlin source code.
 *
 * When using your own styles (i.e. choosing your own color scheme), the style map may contain
 * the following keys :
 *
 * - KEYWORD
 * - SOFTKEYWORD
 * - MODIFIER
 * - ANNOTATION
 * - NUMBER
 * - PAREN
 * - BRACE
 * - BRACKET
 * - SEMICOLON
 * - STRING
 * - COMMENT
 *
 * The default style uses the same highlight for KEYWORD, SOFTKEYWORD and MODIFIER,
 * and therefore the "out" in System.out will appear highlighted.
 * Therefore you may prefer to use a map WITHOUT SOFTKEYWORD or MODIFIER.
 */
open class KotlinSyntax(styles: Map<String, Highlight?>)

    : RegexSyntax(PATTERN, styles) {

    constructor() : this(defaultStyles)

    companion object {

        // The following keywords were taken from :
        // https://kotlinlang.org/docs/reference/keyword-reference.html
        // Note that unlike Java, Kotlin has three shades of keywords, some may be used as identifiers.
        // For example, "out" is a modifier, but can also be used as an identifier (e.g. System.out).
        // This makes it tricky to choose a suitable colour scheme. Using a simple regex, we cannot
        // distinguish between System.out ("out" is an identifier) and List<out Foo> ("out" is a keyword).

        @JvmStatic
        protected val HARD_KEYWORDS = listOf(
                "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "is",
                "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias",
                "val", "var", "when", "while"
        )
        @JvmStatic
        protected val SOFT_KEYWORDS = listOf(
                "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally", "get",
                "import", "init", "param", "property", "receiver", "set", "setparam", "where"
        )
        @JvmStatic
        protected val MODIFIER_KEYWORDS = listOf(
                "abstract", "annotation", "companion", "const", "crossline", "data", "expect", "external",
                "final", "infix", "inline", "inner", "internal", "lateinit", "noinline", "open", "operator",
                "out", "override", "private", "protected", "public", "reified", "sealed", "suspend",
                "tailrec", "vararg"
        )

        @JvmStatic
        protected val HARD_KEYWORD_PATTERN = createKeywordsPattern(HARD_KEYWORDS)

        @JvmStatic
        protected val SOFT_KEYWORD_PATTERN = createKeywordsPattern(SOFT_KEYWORDS)

        @JvmStatic
        protected val MODIFIER_PATTERN = createKeywordsPattern(MODIFIER_KEYWORDS)

        @JvmStatic
        protected val PATTERN = buildPattern(mapOf(
                "KEYWORD" to HARD_KEYWORD_PATTERN,
                "SOFTKEYWORD" to SOFT_KEYWORD_PATTERN,
                "MODIFIER" to MODIFIER_PATTERN,
                "ANNOTATION" to ANNOTATION_PATTERN,
                "NUMBER" to NUMBER_PATTERN,
                "PAREN" to PAREN_PATTERN,
                "BRACE" to BRACE_PATTERN,
                "BRACKET" to BRACKET_PATTERN,
                "SEMICOLON" to SEMICOLON_PATTERN,
                "STRING" to STRING_PATTERN,
                "COMMENT" to COMMENT_PATTERN))

        /**
         * Shares the same style as "HARD" keywords
         */
        @JvmStatic
        val SOFT_KEYWORD_STYLE = "SOFTKEYWORD" to KEYWORD_STYLE.second

        /**
         * Shares the same style as "HARD" keywords
         */
        @JvmStatic
        val MODIFIER_STYLE = "MODIFIER" to KEYWORD_STYLE.second

        @JvmStatic
        val SEMICOLON_STYLE = "SEMICOLON" to StyleHighlight("-fx-fill: #ff3333;")

        @JvmStatic
        val defaultStyles = hashMapOf(
                KEYWORD_STYLE, SOFT_KEYWORD_STYLE, MODIFIER_STYLE,
                NUMBER_STYLE, ANNOTATION_STYLE,
                PAREN_STYLE, BRACE_STYLE, BRACKET_STYLE, SEMICOLON_STYLE,
                STRING_STYLE, COMMENT_STYLE
        )

        @JvmStatic
        val instance = KotlinSyntax()
    }
}
