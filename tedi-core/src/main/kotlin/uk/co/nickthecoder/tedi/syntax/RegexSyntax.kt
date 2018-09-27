package uk.co.nickthecoder.tedi.syntax

import uk.co.nickthecoder.tedi.Highlight
import uk.co.nickthecoder.tedi.HighlightRange
import uk.co.nickthecoder.tedi.StyleClassHighlight
import java.util.regex.Pattern

/**
 * This code was originally from
 * [RichTextFX](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/JavaKeywordsDemo.java)
 * but has been radically changed.
 */
open class RegexSyntax(val list: List<RegexHighlight>)

    : Syntax() {

    val pattern = buildPattern(list.map { it.name to it.pattern }.toMap())

    val styleMap = list.map { it.name to it.highlight }.toMap()

    /**
     * Create a list of [HighlightRange]s suitable for styling source code.
     *
     * Note, I have seen StackOverflowErrors being thrown by java.util.regex.Pattern,
     * when the string contains unmatched double quotes at the end of a line.
     * Maybe it's a bug in Pattern, which I can't do anything about!
     * Maybe you can form a better Pattern, which is not susceptible to this problem.
     * Nothing crashes though (I catch it).
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

        val NUMBER_PATTERN = "[0-9](\\.[0-9]+)?"
        val ANNOTATION_PATTERN = "\\@\\b\\w+\\b" // e.g. @Deprecated
        val PAREN_PATTERN = "[\\(\\)]+" // Note "()" will form ONE range, not two.
        val BRACE_PATTERN = "[\\{\\}]+" // Note "{}" will form ONE range, not two.
        val BRACKET_PATTERN = "[\\[\\]]+" // Note "[]" will form ONE range, not two.
        val SEMICOLON_PATTERN = "\\;"
        val SEMICOLON_EOL_PATTERN = "\\;\\s*$"
        val STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'])*'" // Both single and double quotes.
        val COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"

        /**
         * Takes a list of keywords, such as "class", "interface" etc, and converts it to a
         * pattern suitable for use with Pattern.compile.
         */
        fun createKeywordsPattern(keywords: List<String>): String {
            return keywords.joinToString(prefix = "\\b(", separator = "|", postfix = ")\\b")
        }

        /**
         * Builds a big Pattern!
         *
         * The result is something like : (?<GROUP1>GROUP1PATTERN))|(?<GROUP2>GROUP2PATTERN)
         *
         * @param items A pair of group names (first) and pattern strings (second).
         *
         */
        fun buildPattern(items: Map<String, String>): Pattern {
            val patternString = items.map { "(?<" + it.key + ">" + it.value + ")" }.joinToString(separator = "|")
            return Pattern.compile(patternString, Pattern.MULTILINE)
        }

    }

}

/**
 * A simple triple, of :
 * - [name] : which is used as the name of a group in the regex [Pattern].
 * - [pattern] : A regex pattern (a string, not a compiled [Pattern].
 * - [highlight] : A [Highlight], the default is to use a [StyleClassHighlight],
 *   with a css style class of "syntax-" + [name].
 */
data class RegexHighlight(
        val name: String,
        val pattern: String,
        val highlight: Highlight = StyleClassHighlight("syntax-$name")
)

