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
