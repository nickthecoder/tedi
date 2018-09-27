/*
 * Loosely Based off of RichTextFX's Java syntax highlighter :
 *
 * https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/JavaKeywordsDemo.java
 */
package uk.co.nickthecoder.tedi.syntax

import uk.co.nickthecoder.tedi.Highlight
import uk.co.nickthecoder.tedi.StyleHighlight
import java.util.regex.Pattern

/**
 * Creates HighlightRanges suitable for Kotlin source code.
 *
 * When using your own styles (i.e. choosing your own color scheme), the style map MUST contain
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

    : RegexSyntax(JAVA_PATTERN, styles) {

    constructor() : this(defaultStyles)

    companion object {

        protected val JAVA_KEYWORDS = listOf(
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
                "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
                "for", "goto", "if", "implements", "import", "inner", "instanceof", "int", "interface", "long", "native",
                "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
                "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try",
                "void", "volatile", "while"
        )

        protected val JAVA_KEYWORD_PATTERN = createKeywordsPattern(JAVA_KEYWORDS)

        protected val JAVA_PATTERN: Pattern = Pattern.compile(
                "(?<KEYWORD>" + JAVA_KEYWORD_PATTERN + ")"
                        + "|(?<PAREN>" + PAREN_PATTERN + ")"
                        + "|(?<BRACE>" + BRACE_PATTERN + ")"
                        + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                        + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                        + "|(?<STRING>" + STRING_PATTERN + ")"
                        + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
        )

        val defaultStyles = hashMapOf(
                "KEYWORD" to StyleHighlight("-fx-fill: #000080;"),
                "PAREN" to StyleHighlight("-fx-fill: #cc33cc;"),
                "BRACE" to StyleHighlight("-fx-fill: #33cccc;"),
                "BRACKET" to StyleHighlight("-fx-fill: #ddaa00;"),
                "SEMICOLON" to StyleHighlight("-fx-fill: #997799;"),
                "STRING" to StyleHighlight("-fx-fill: #008000;"),
                "COMMENT" to StyleHighlight("-fx-fill: #808080;")
        )

        @JvmStatic
        val instance = JavaSyntax()

    }

}
