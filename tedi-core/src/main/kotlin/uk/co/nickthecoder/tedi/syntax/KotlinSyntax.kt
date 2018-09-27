package uk.co.nickthecoder.tedi.syntax

import uk.co.nickthecoder.tedi.Highlight
import java.util.regex.Pattern

/**
 * Creates HighlightRanges suitable for Kotlin source code.
 *
 * When using your own styles (i.e. choosing your own color scheme), the style map MUST contain
 * the following keys :
 *
 * - "KEYWORD"
 * - "SOFTKEYWORD"
 * - "MODIFIER" "PAREN"
 * - "BRACE"
 * - "BRACKET"
 * - "SEMICOLON"
 * - "STRING"
 * - "COMMENT"
 */
open class KotlinSyntax(styles: Map<String, Highlight?>)

    : RegexSyntax(KOTLIN_PATTERN, styles) {

    constructor() : this(defaultStyles)

    companion object {

        // The following keywords were taken from :
        // https://kotlinlang.org/docs/reference/keyword-reference.html
        // Note that unlike Java, Kotlin has three shades of keywords, some may be used as identifiers.
        protected val KOTLIN_HARD_KEYWORDS = listOf(
                "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "is",
                "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias",
                "val", "var", "when", "while"
        )
        protected val KOTLIN_SOFT_KEYWORDS = listOf(
                "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally", "get",
                "import", "init", "param", "property", "receiver", "set", "setparam", "where"
        )

        protected val KOTLIN_MODIFIER_KEYWORDS = listOf(
                "abstract", "annotation", "companion", "const", "crossline", "data", "expect", "external",
                "final", "infix", "inline", "inner", "internal", "lateinit", "noinline", "open", "operator",
                "out", "override", "private", "protected", "public", "reified", "sealed", "suspend",
                "tailrec", "vararg"
        )

        protected val KOTLIN_HARD_PATTERN = createKeywordsPattern(KOTLIN_HARD_KEYWORDS)
        protected val KOTLIN_SOFT_PATTERN = createKeywordsPattern(KOTLIN_SOFT_KEYWORDS)
        protected val KOTLIN_MODIFIER_PATTERN = createKeywordsPattern(KOTLIN_MODIFIER_KEYWORDS)

        protected val KOTLIN_PATTERN: Pattern = Pattern.compile(
                "(?<KEYWORD>" + KOTLIN_HARD_PATTERN + ")"
                        + "|(?<SOFTKEYWORD>" + KOTLIN_SOFT_PATTERN + ")"
                        + "|(?<MODIFIER>" + KOTLIN_MODIFIER_PATTERN + ")"
                        + "|(?<PAREN>" + PAREN_PATTERN + ")"
                        + "|(?<BRACE>" + BRACE_PATTERN + ")"
                        + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                        + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                        + "|(?<STRING>" + STRING_PATTERN + ")"
                        + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
        )

        /**
         * Shares the same style as "HARD" keywords
         */
        val SOFT_KEYWORD_STYLE = "SOFTKEYWORD" to KEYWORD_STYLE.second

        /**
         * Shares the same style as "HARD" keywords
         */
        val MODIFIER_STYLE = "MODIFIER" to KEYWORD_STYLE.second

        val defaultStyles = hashMapOf(
                KEYWORD_STYLE, SOFT_KEYWORD_STYLE, MODIFIER_STYLE, PAREN_STYLE, BRACE_STYLE,
                BRACKET_STYLE, SEMICOLON_STYLE, STRING_STYLE, COMMENT_STYLE
        )

        @JvmStatic
        val instance = KotlinSyntax()
    }
}
