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
 * Creates HighlightRanges suitable for Groovy source code.
 */
open class GroovySyntax()
    : RegexSyntax(listOf(
        KEYWORD, NUMBER, C_COMMENT, ANNOTATION, STRING,
        OPEN_PAREN, CLOSE_PAREN, OPEN_BRACE, CLOSE_BRACE, OPEN_BRACKET, CLOSE_BRACKET,
        WASTEFUL_SEMICOLON)) {

    companion object {

        /**
         * Taken from :
         * http://docs.groovy-lang.org/latest/html/documentation/
         */
        @JvmStatic
        val KEYWORDS = listOf(
                "as", "assert", "break", "case", "catch", "class", "const", "continue",
                "def", "default", "do", "else", "enum", "extends", "false", "finally", "for", "goto", "if",
                "implements", "import", "in", "instanceof", "interface", "new", "null", "package", "return",
                "super", "switch", "this", "throw", "throws", "trait", "true", "try", "while")


        @JvmStatic
        val KEYWORD_PATTERN = createKeywordsPattern(KEYWORDS)

        val KEYWORD = RegexHighlight("keyword", JavaSyntax.KEYWORD_PATTERN)

        @JvmStatic
        val instance = GroovySyntax()
    }
}
