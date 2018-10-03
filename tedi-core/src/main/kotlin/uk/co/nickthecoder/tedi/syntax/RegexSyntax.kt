package uk.co.nickthecoder.tedi.syntax

import uk.co.nickthecoder.tedi.*
import java.util.regex.Pattern

/**
 * Parses text using a regular expression, creating [HighlightRange]s for different parts of the text
 * (such as comments, keywords etc).
 *
 * It also does something "clever" with parts of the text which should match, such as open/close brackets.
 * Instead of a simple [HighlightRange], it creates [PairedHighlightRange].
 * If un-matched pairs are found, then the first mis-match is given an [ERROR] highlight.
 *
 * [TediArea] does nothing special with [PairedHighlightRange], but
 * [HighlightMatchedPairs] uses the extra information in [PairedHighlightRange] to highlight both parts of the pair,
 * when we move the caret over these ranges. Therefore the programmer can instantly see where (, [ and { are matched
 * with }, ] and ), and visa-versa.
 *
 * Your code could use [HighlightMatchedPairs] in other ways, for example, double click one to jump to the other
 * half of the pair.
 *
 * This code was originally from
 * [RichTextFX](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/JavaKeywordsDemo.java)
 * but has been radically changed. Only the basic idea remains, even the regex expressions have changed.
 */
open class RegexSyntax(val list: List<RegexHighlight>)

    : Syntax() {

    val pattern = buildPattern(list.map { it.name to it.pattern }.toMap())

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

        fun findRegexHighlight(): RegexHighlight? {
            for (item in list) {
                if (matcher.group(item.name) != null) {
                    return item
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

        // Keep track of open brackets, open braces etc, which have not been closed
        val unmatched = mutableListOf<Pair<RegexHighlight, HighlightRange>>()

        // Once we find one unmatched pair, then ignore the rest, as they will only confuse the issue!
        var allMatched = true

        while (catchMatcherFind()) {
            findRegexHighlight()?.let { regexHighlight ->

                var range = HighlightRange(matcher.start(), matcher.end(), regexHighlight.highlight, this)

                if (allMatched) {

                    // Have we found an open bracket etc?
                    val closing = regexHighlight.closingRegexHighlight
                    if (closing != null) {
                        unmatched.add(Pair(regexHighlight, range))
                    }

                    // Have we found a close bracket etc?
                    val opening = regexHighlight.openingRegexHighlight
                    if (opening != null) {

                        val top = unmatched.lastOrNull()
                        if (top?.first == opening) {
                            // We've found a matching pair. Let's join them together

                            // Upgrade the opening range, (which is a simple HighlightRange), with a PairedHighlightRange.
                            val oldRange = top.second
                            val openingRange = PairedHighlightRange(oldRange.start, oldRange.end, oldRange.highlight, null)
                            ranges[ranges.indexOf(oldRange)] = openingRange

                            // The current range is upgrade to a PairedHighlightRange too.
                            range = PairedHighlightRange(range.start, range.end, range.highlight, openingRange)

                            // Pop off the stack, now that it has been matched.
                            unmatched.removeAt(unmatched.size - 1)

                        } else {
                            // Oops, this is mis-matched.
                            // Stop further checking for matches
                            allMatched = false
                            // Use a different Highlight, so that it's obvious that this is unmatched.
                            range = HighlightRange(range.start, range.end, ERROR_HIGHLIGHT, this)
                        }

                    }
                }
                ranges.add(range)
            }
        }

        // There are unmatched opens. If so, change their highlights to indicate an ERROR.
        if (allMatched && unmatched.isNotEmpty()) {
            for ((_, range) in unmatched) {
                val replacement = HighlightRange(range.start, range.end, ERROR_HIGHLIGHT, this)
                ranges[ranges.indexOf(range)] = replacement
            }
        }

        return ranges
    }


    companion object {

        val NUMBER_PATTERN = "\\b[0-9](\\.[0-9]+)?\\b"
        val ANNOTATION_PATTERN = "\\@\\b\\w+\\b" // e.g. @Deprecated
        val SEMICOLON_PATTERN = "\\;"
        val SEMICOLON_EOL_PATTERN = "\\;\\s*$"
        val STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'])*'" // Both single and double quotes.
        val C_COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/" // Comments for C-like languages

        val OPEN_PAREN_PATTERN = "\\("
        val CLOSE_PAREN_PATTERN = "\\)"
        val OPEN_BRACKET_PATTERN = "\\["
        val CLOSE_BRACKET_PATTERN = "\\]"
        val OPEN_BRACE_PATTERN = "\\{"
        val CLOSE_BRACE_PATTERN = "\\}"

        val NUMBER = RegexHighlight("number", NUMBER_PATTERN)
        val ANNOTATION = RegexHighlight("annotation", ANNOTATION_PATTERN)
        val SEMICOLON = RegexHighlight("semicolon", SEMICOLON_PATTERN)
        val STRING = RegexHighlight("string", STRING_PATTERN)
        val C_COMMENT = RegexHighlight("comment", C_COMMENT_PATTERN)

        // For Kotlin and Groovy, where trailing ; are not needed.
        val WASTEFUL_SEMICOLON = RegexHighlight("wastefulSemicolon", SEMICOLON_EOL_PATTERN, FillStyleClassHighlight("syntax-error"))

        val OPEN_PAREN = RegexHighlight("openparen", OPEN_PAREN_PATTERN, StyleClassHighlight("syntax-paren"))
        val OPEN_BRACKET = RegexHighlight("openbracket", OPEN_BRACKET_PATTERN, StyleClassHighlight("syntax-bracket"))
        val OPEN_BRACE = RegexHighlight("openbrace", OPEN_BRACE_PATTERN, StyleClassHighlight("syntax-brace"))

        val CLOSE_PAREN = RegexHighlight("closeparen", CLOSE_PAREN_PATTERN, OPEN_PAREN, StyleClassHighlight("syntax-paren"))
        val CLOSE_BRACKET = RegexHighlight("closebracket", CLOSE_BRACKET_PATTERN, OPEN_BRACKET, StyleClassHighlight("syntax-bracket"))
        val CLOSE_BRACE = RegexHighlight("closebrace", CLOSE_BRACE_PATTERN, OPEN_BRACE, StyleClassHighlight("syntax-brace"))

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
class RegexHighlight(
        val name: String,
        val pattern: String,
        /**
         * e.g. for a closing bracket, this would be the RegexHighlight for an opening bracket.
         */
        val openingRegexHighlight: RegexHighlight? = null,
        val highlight: Highlight = StyleClassHighlight("syntax-$name")
) {

    constructor(name: String, pattern: String, highlight: Highlight) : this(name, pattern, null, highlight)

    var closingRegexHighlight: RegexHighlight? = null

    init {
        if (openingRegexHighlight != null) {
            openingRegexHighlight.closingRegexHighlight = this
        }
    }

    override fun toString() = name + if (openingRegexHighlight != null) " close" else if (closingRegexHighlight != null) " open" else ""
}
