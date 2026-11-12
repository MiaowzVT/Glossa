package de.miaowz.glossa.richtext

import android.text.Spannable
import android.text.Spanned

/**
 * Editing operations on [RichFormatSpan]s. Every format kind is kept non-overlapping and
 * adjacent spans of equal format are merged, so the saved span list stays small.
 */
object SpanOps {

    private fun Spanned.richSpans(kind: FormatKind, start: Int, end: Int): List<RichFormatSpan> =
        getSpans(start, end, RichFormatSpan::class.java).filter { it.format.kind == kind }

    /** Formats active on every character of [start, end), or at the cursor if the range is empty. */
    fun formatsIn(text: Spanned, start: Int, end: Int): Map<FormatKind, SpanFormat> {
        if (start == end) return formatsAt(text, start)
        return FormatKind.entries.mapNotNull { kind ->
            val spans = text.richSpans(kind, start, end)
                .filter { text.getSpanStart(it) < end && text.getSpanEnd(it) > start }
            val format = spans.firstOrNull()?.format ?: return@mapNotNull null
            val covered = spans.filter { it.format == format }
                .map { text.getSpanStart(it) to text.getSpanEnd(it) }
            if (coversRange(covered, start, end)) kind to format else null
        }.toMap()
    }

    /** Formats that text typed at [position] should inherit (taken from the preceding character). */
    fun formatsAt(text: Spanned, position: Int): Map<FormatKind, SpanFormat> {
        if (text.isEmpty()) return emptyMap()
        val index = if (position > 0) position - 1 else 0
        return text.getSpans(index, index + 1, RichFormatSpan::class.java)
            .filter { text.getSpanStart(it) <= index && text.getSpanEnd(it) > index }
            .associate { it.format.kind to it.format }
    }

    /** Applies [format] to [start, end), replacing other formats of the same kind there. */
    fun apply(text: Spannable, format: SpanFormat, start: Int, end: Int) {
        if (start >= end) return
        remove(text, format.kind, start, end)
        var mergedStart = start
        var mergedEnd = end
        // Spans touching the range boundaries can be merged with the new span.
        for (span in text.richSpans(format.kind, maxOf(0, start - 1), minOf(text.length, end + 1))) {
            if (span.format != format) continue
            val spanStart = text.getSpanStart(span)
            val spanEnd = text.getSpanEnd(span)
            if (spanEnd == start || spanStart == end) {
                mergedStart = minOf(mergedStart, spanStart)
                mergedEnd = maxOf(mergedEnd, spanEnd)
                text.removeSpan(span)
            }
        }
        text.setSpan(format.createSpan(), mergedStart, mergedEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    /** Removes formats of [kind] from [start, end), keeping the parts of spans outside the range. */
    fun remove(text: Spannable, kind: FormatKind, start: Int, end: Int) {
        if (start >= end) return
        for (span in text.richSpans(kind, start, end)) {
            val spanStart = text.getSpanStart(span)
            val spanEnd = text.getSpanEnd(span)
            if (spanEnd <= start || spanStart >= end) continue
            text.removeSpan(span)
            if (spanStart < start) {
                text.setSpan(span.format.createSpan(), spanStart, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (spanEnd > end) {
                text.setSpan(span.format.createSpan(), end, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    fun clear(text: Spannable, start: Int, end: Int) {
        FormatKind.entries.forEach { remove(text, it, start, end) }
    }

    private fun coversRange(ranges: List<Pair<Int, Int>>, start: Int, end: Int): Boolean {
        var reached = start
        for ((rangeStart, rangeEnd) in ranges.sortedBy { it.first }) {
            if (rangeStart > reached) return false
            reached = maxOf(reached, rangeEnd)
            if (reached >= end) return true
        }
        return reached >= end
    }
}
