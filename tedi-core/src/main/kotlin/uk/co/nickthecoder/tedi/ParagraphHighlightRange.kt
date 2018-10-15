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
package uk.co.nickthecoder.tedi

/**
 * Used internally by TediArea to help spilt Paragraphs into pieces, where each piece is
 * highlighted in a different manner.
 * The start and end of the [HighlightRange] are ignored after the HighlightSlice is created,
 * and are only kept so that when [HighlightRange]s are removed, it is easy to tell which
 * slices can be removed or merged.
 */
internal class ParagraphHighlightRange(
        var startColumn: Int,
        var endColumn: Int,
        val cause: HighlightRange) {

    fun intersects(from: Int, to: Int): Boolean = from >= startColumn && to <= endColumn

    override fun toString() = "PHR $startColumn..$endColumn from $cause"
}
