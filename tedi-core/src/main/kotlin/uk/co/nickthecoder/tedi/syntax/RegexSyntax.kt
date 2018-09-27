package uk.co.nickthecoder.tedi.syntax

import uk.co.nickthecoder.tedi.Highlight
import uk.co.nickthecoder.tedi.HighlightRange
import java.util.regex.Pattern

open class RegexSyntax(val pattern: Pattern, val styleMap: Map<String, Highlight?>)

    : Syntax() {

    /**
     * Create a list of [HighlightRange]s suitable for styling source code.
     *
     * Note, I have seen StackOverflowErrors being thrown by java.util.regex.Pattern,
     * when the string contains unmatched double quotes at the end of a line.
     * Maybe it's a bug in Pattern, which I can't do anything about!
     * Maybe you can form a better Pattern, which is not susceptible to this problem.
     * Nothing crashes though (as I run this on a separate thread),
     * and if you type another letter, the document is highlighted
     * (all in green, due to the unmatched double quote).
     * Closing the quote, and everything is back to normal. ;-)
     */
    override fun createRanges(text: String): List<HighlightRange> {

        val matcher = pattern.matcher(text)
        val ranges = mutableListOf<HighlightRange>()

        /**
         * Don't you just love nested functions. Does Java have these yet? (It's been ages since I wrote in Java).
         * Ah wait, does Java even HAVE functions?
         */
        fun findHighlight(): Highlight? {
            for (key in styleMap.keys) {
                if (matcher.group(key) != null) {
                    return styleMap[key]
                }
            }
            return null
        }

        /**
         * See the comment above. Exit as gracefully as I can when matcher throws an error.
         */
        fun catchMatcherFind(): Boolean {
            try {
                return matcher.find()
            } catch (e: Throwable) {
                return false
            }
        }

        while (catchMatcherFind()) {
            findHighlight()?.let { ranges.add(HighlightRange(matcher.start(), matcher.end(), it, this)) }
        }

        return ranges
    }


    companion object {

        val PAREN_PATTERN = "\\(|\\)"
        val BRACE_PATTERN = "\\{|\\}"
        val BRACKET_PATTERN = "\\[|\\]"
        val SEMICOLON_PATTERN = "\\;"
        val STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\""
        val COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"

        /**
         * Takes a list of keywords, such as "class", "interface" etc, and converts it to a
         * pattern suitable for use with Pattern.compile.
         */
        fun createKeywordsPattern(keywords: List<String>): String {
            return keywords.joinToString(prefix = "\\b(", separator = "|", postfix = ")\\b")
        }

    }

}
