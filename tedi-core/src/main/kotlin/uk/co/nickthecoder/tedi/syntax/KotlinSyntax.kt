/*
Tedi
Copyright (C) 2018 Nick Robinson

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2 only, as
published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
package uk.co.nickthecoder.tedi.syntax

/**
 * Creates HighlightRanges suitable for Kotlin source code.
 *
 * The default style uses the same highlight for KEYWORD, SOFTKEYWORD and MODIFIER,
 * and therefore the "out" in System.out will appear highlighted.
 * Therefore you may prefer to use a map WITHOUT SOFTKEYWORD or MODIFIER.
 */
open class KotlinSyntax()

    : RegexSyntax(listOf(
        HARD_KEYWORD, SOFT_KEYWORD, MODIFIER, NUMBER, C_COMMENT, ANNOTATION, STRING,
        OPEN_PAREN, CLOSE_PAREN, OPEN_BRACE, CLOSE_BRACE, OPEN_BRACKET, CLOSE_BRACKET,
        WASTEFUL_SEMICOLON)) {

    companion object {

        // The following keywords were taken from :
        // https://kotlinlang.org/docs/reference/keyword-reference.html
        // Note that unlike Java, Kotlin has three shades of keywords, some may be used as identifiers.
        // For example, "out" is a modifier, but can also be used as an identifier (e.g. System.out).
        // This makes it tricky to choose a suitable colour scheme. Using a simple regex, we cannot
        // distinguish between System.out ("out" is an identifier) and List<out Foo> ("out" is a keyword).

        @JvmStatic
        val HARD_KEYWORDS = listOf(
                "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "is",
                "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias",
                "val", "var", "when", "while"
        )
        @JvmStatic
        val SOFT_KEYWORDS = listOf(
                "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally", "get",
                "import", "init", "param", "property", "receiver", "set", "setparam", "where"
        )
        @JvmStatic
        val MODIFIER_KEYWORDS = listOf(
                "abstract", "annotation", "companion", "const", "crossline", "data", "expect", "external",
                "final", "infix", "inline", "inner", "internal", "lateinit", "noinline", "open", "operator",
                "out", "override", "private", "protected", "public", "reified", "sealed", "suspend",
                "tailrec", "vararg"
        )

        @JvmStatic
        val HARD_KEYWORD_PATTERN = createKeywordsPattern(HARD_KEYWORDS)

        @JvmStatic
        val SOFT_KEYWORD_PATTERN = createKeywordsPattern(SOFT_KEYWORDS)

        @JvmStatic
        val MODIFIER_PATTERN = createKeywordsPattern(MODIFIER_KEYWORDS)

        val HARD_KEYWORD = RegexHighlight("keyword", HARD_KEYWORD_PATTERN)
        val SOFT_KEYWORD = RegexHighlight("softKeyword", SOFT_KEYWORD_PATTERN)
        val MODIFIER = RegexHighlight("modifier", MODIFIER_PATTERN)

        @JvmStatic
        val instance = KotlinSyntax()
    }
}
