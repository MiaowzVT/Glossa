package de.miaowz.glossa.richtext

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan

/**
 * Marks spans created by the rich text editor. Only these are persisted, so foreign spans
 * (IME composing underlines, spell checker spans, ...) never end up in the saved [RichText].
 */
interface RichFormatSpan {
    val format: SpanFormat
}

private class BoldSpan : StyleSpan(Typeface.BOLD), RichFormatSpan {
    override val format get() = SpanFormat.Bold
}

private class ItalicSpan : StyleSpan(Typeface.ITALIC), RichFormatSpan {
    override val format get() = SpanFormat.Italic
}

private class RichUnderlineSpan : UnderlineSpan(), RichFormatSpan {
    override val format get() = SpanFormat.Underline
}

private class RichStrikethroughSpan : StrikethroughSpan(), RichFormatSpan {
    override val format get() = SpanFormat.Strikethrough
}

private class RichTextColorSpan(override val format: SpanFormat.TextColor) :
    ForegroundColorSpan(format.argb), RichFormatSpan

private class RichHighlightSpan(override val format: SpanFormat.Highlight) :
    BackgroundColorSpan(format.argb), RichFormatSpan

private class RichSizeSpan(override val format: SpanFormat.RelativeSize) :
    RelativeSizeSpan(format.proportion), RichFormatSpan

fun SpanFormat.createSpan(): CharacterStyle = when (this) {
    SpanFormat.Bold -> BoldSpan()
    SpanFormat.Italic -> ItalicSpan()
    SpanFormat.Underline -> RichUnderlineSpan()
    SpanFormat.Strikethrough -> RichStrikethroughSpan()
    is SpanFormat.TextColor -> RichTextColorSpan(this)
    is SpanFormat.Highlight -> RichHighlightSpan(this)
    is SpanFormat.RelativeSize -> RichSizeSpan(this)
}

fun RichText.toSpannable(): SpannableStringBuilder {
    val builder = SpannableStringBuilder(text)
    for (span in spans) {
        builder.setSpan(span.format.createSpan(), span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return builder
}

fun Spanned.toRichText(): RichText {
    val spans = getSpans(0, length, RichFormatSpan::class.java)
        .map { RichSpan(getSpanStart(it), getSpanEnd(it), it.format) }
        .filter { it.start < it.end }
        .sortedWith(compareBy({ it.start }, { it.end }))
    return RichText(toString(), spans)
}
