package uk.co.nickthecoder.tedi.syntax

import uk.co.nickthecoder.tedi.Highlight
import uk.co.nickthecoder.tedi.HighlightRange
import uk.co.nickthecoder.tedi.StyleHighlight


// The following keywords were taken from :
// https://kotlinlang.org/docs/reference/keyword-reference.html
// Note that unlike Java, Kotlin has three shades of keywords, some may be used as identifiers.
val KOTLIN_HARD_KEYWORDS = listOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "is",
        "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias",
        "val", "var", "when", "while"
)
val KOTLIN_SOFT_KEYWORDS = listOf(
        "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally", "get",
        "import", "init", "param", "property", "receiver", "set", "setparam", "where"
)

val KOTLIN_MODIFIER_KEYWORDS = listOf(
        "abstract", "annotation", "companion", "const", "crossline", "data", "expect", "external",
        "final", "infix", "inline", "inner", "internal", "lateinit", "noinline", "open", "operator",
        "out", "override", "private", "protected", "public", "reified", "sealed", "suspend",
        "tailrec", "vararg"
)

val KOTLIN_HARD_PATTERN = createKeywordsPattern(KOTLIN_HARD_KEYWORDS)
val KOTLIN_SOFT_PATTERN = createKeywordsPattern(KOTLIN_SOFT_KEYWORDS)
val KOTLIN_MODIFIER_PATTERN = createKeywordsPattern(KOTLIN_MODIFIER_KEYWORDS)


val defaultKotlinStyleMap = hashMapOf(
        "KEYWORD" to StyleHighlight("-fx-fill: #000080;"),
        "SOFTKEYWORD" to StyleHighlight("-fx-fill: #000080;"),
        "MODIFIER" to StyleHighlight("-fx-fill: #000080;"),
        "PAREN" to StyleHighlight("-fx-fill: #cc33cc;"),
        "BRACE" to StyleHighlight("-fx-fill: #33cccc;"),
        "BRACKET" to StyleHighlight("-fx-fill: #ddaa00;"),
        "SEMICOLON" to StyleHighlight("-fx-fill: #ff6666;"),
        "STRING" to StyleHighlight("-fx-fill: #008000;"),
        "COMMENT" to StyleHighlight("-fx-fill: #808080;")
)

/**
 * Create a list of [HighlightRange]s suitable for styling kotlin source code.
 *
 * Gives full control of the highlight colours etc, by passing in your own map of String->Highlight.
 * The keys must be KEYWORD, SOFTKEYWORD, MODIFIER, PAREN, BRACE, BRACKET, SEMICOLON, STRING, COMMENT
 */
fun kotlinSyntax(text: String, styleMap: Map<String, Highlight?>) = syntax(text, KOTLIN_PATTERN, styleMap)

/**
 * Create a list of [HighlightRange]s suitable for styling source code.
 *
 * Uses a default set of styles, which may not suite your tastes.
 */
fun kotlinSyntax(text: String) = syntax(text, KOTLIN_PATTERN, defaultKotlinStyleMap)
