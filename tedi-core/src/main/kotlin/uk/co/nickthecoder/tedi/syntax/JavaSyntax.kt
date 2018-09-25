/*
 * Based off of RichTextFX's Java syntax highlighter :
 *
 * https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/JavaKeywordsDemo.java
 */
package uk.co.nickthecoder.tedi.syntax

import uk.co.nickthecoder.tedi.Highlight
import uk.co.nickthecoder.tedi.HighlightRange
import uk.co.nickthecoder.tedi.StyleHighlight
import java.util.regex.Pattern

val JAVA_KEYWORDS = listOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "inner", "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try",
        "void", "volatile", "while"
)


val JAVA_KEYWORD_PATTERN = createKeywordsPattern(JAVA_KEYWORDS)

val JAVA_PATTERN: Pattern = Pattern.compile(
        "(?<KEYWORD>" + JAVA_KEYWORD_PATTERN + ")"
                + "|(?<PAREN>" + PAREN_PATTERN + ")"
                + "|(?<BRACE>" + BRACE_PATTERN + ")"
                + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                + "|(?<STRING>" + STRING_PATTERN + ")"
                + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
)

val KOTLIN_PATTERN: Pattern = Pattern.compile(
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

val defaultJavaStyleMap = hashMapOf(
        "KEYWORD" to StyleHighlight("-fx-fill: #000080;"),
        "PAREN" to StyleHighlight("-fx-fill: #cc33cc;"),
        "BRACE" to StyleHighlight("-fx-fill: #33cccc;"),
        "BRACKET" to StyleHighlight("-fx-fill: #ddaa00;"),
        "SEMICOLON" to StyleHighlight("-fx-fill: #997799;"),
        "STRING" to StyleHighlight("-fx-fill: #008000;"),
        "COMMENT" to StyleHighlight("-fx-fill: #808080;")
)


/**
 * Create a list of [HighlightRange]s suitable for styling Java source code.
 *
 * Gives full control of the highlight colours etc, by passing in your own map of String->Highlight.
 * The keys must be KEYWORD, PAREN, BRACE, BRACKET, SEMICOLON, STRING, COMMENT
 */
fun javaSyntax(text: String, styleMap: Map<String, Highlight?>) = syntax(text, JAVA_PATTERN, styleMap)

/**
 * Create a list of [HighlightRange]s suitable for styling Java source code.
 *
 * Uses a default set of styles, which may not suite your tastes.
 * For you Java fans, you can call it like so (assuming you import uk.co.nickthecoder.tedi.JavaSyntaxKt) :
 *
 *     JavaSyntaxKt.javaSyntax( text )
 *
 */
fun javaSyntax(text: String) = javaSyntax(text, defaultJavaStyleMap.filterKeys { it != "MODIFIER" && it != "SOFTKEYWORD" })
