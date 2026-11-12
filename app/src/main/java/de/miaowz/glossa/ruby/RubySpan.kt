package de.miaowz.glossa.ruby

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Spannable
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.MetricAffectingSpan
import android.text.style.ReplacementSpan
import kotlin.math.ceil
import kotlin.math.max

/**
 * Draws [base] with [reading] centered above it (ruby annotation / furigana).
 *
 * The span replaces the whole markup (`{base|reading}`). Because TextLine does not apply
 * non-metric styles (color, underline, ...) to replacement spans, the styles of the base text
 * are passed in as [baseStyles] and applied here. They always cover the whole base.
 */
class RubySpan(
    val base: String,
    val reading: String,
    private val baseStyles: List<CharacterStyle> = emptyList(),
    private val rubyScale: Float = 0.5f,
) : ReplacementSpan() {

    private val backgroundPaint = Paint()

    private fun basePaint(paint: Paint): TextPaint = TextPaint(paint).apply {
        bgColor = 0
        for (style in baseStyles) {
            if (style is MetricAffectingSpan) style.updateMeasureState(this)
            style.updateDrawState(this)
        }
    }

    private fun rubyPaint(basePaint: TextPaint): TextPaint = TextPaint(basePaint).apply {
        textSize = basePaint.textSize * rubyScale
        isUnderlineText = false
        isStrikeThruText = false
        bgColor = 0
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        val basePaint = basePaint(paint)
        val rubyPaint = rubyPaint(basePaint)
        if (fm != null) {
            val baseMetrics = basePaint.fontMetricsInt
            val rubyMetrics = rubyPaint.fontMetricsInt
            val rubyHeight = rubyMetrics.descent - rubyMetrics.ascent
            fm.ascent = baseMetrics.ascent - rubyHeight
            fm.top = minOf(baseMetrics.top, fm.ascent)
            fm.descent = baseMetrics.descent
            fm.bottom = baseMetrics.bottom
            fm.leading = baseMetrics.leading
        }
        return ceil(max(basePaint.measureText(base), rubyPaint.measureText(reading))).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val basePaint = basePaint(paint)
        val rubyPaint = rubyPaint(basePaint)
        val baseWidth = basePaint.measureText(base)
        val rubyWidth = rubyPaint.measureText(reading)
        val width = max(baseWidth, rubyWidth)
        val baseMetrics = basePaint.fontMetrics

        if (basePaint.bgColor != 0) {
            backgroundPaint.color = basePaint.bgColor
            canvas.drawRect(x, y + baseMetrics.ascent, x + width, y + baseMetrics.descent, backgroundPaint)
        }
        canvas.drawText(base, x + (width - baseWidth) / 2, y.toFloat(), basePaint)
        val rubyBaseline = y + baseMetrics.ascent - rubyPaint.fontMetrics.descent
        canvas.drawText(reading, x + (width - rubyWidth) / 2, rubyBaseline, rubyPaint)
    }

    companion object {
        /**
         * Replaces all ruby markup in [text] with [RubySpan]s. Character styles overlapping a
         * markup are cut out of it; the ones touching the base text are moved into the span.
         */
        fun applyTo(text: Spannable) {
            for (match in RubyMarkup.findAll(text)) {
                val start = match.range.first
                val end = match.range.last + 1
                val baseStyles = mutableListOf<CharacterStyle>()
                for (style in text.getSpans(start, end, CharacterStyle::class.java)) {
                    val styleStart = text.getSpanStart(style)
                    val styleEnd = text.getSpanEnd(style)
                    if (style is ReplacementSpan || styleEnd <= start || styleStart >= end) continue
                    if (styleStart < match.baseEnd && styleEnd > match.baseStart) baseStyles += style
                    val flags = text.getSpanFlags(style)
                    text.removeSpan(style)
                    if (styleStart < start) text.setSpan(style, styleStart, start, flags)
                    if (styleEnd > end) {
                        // A span object can only be attached once; wrap it for the trailing part.
                        val trailing = if (styleStart < start) CharacterStyle.wrap(style) else style
                        text.setSpan(trailing, end, styleEnd, flags)
                    }
                }
                text.setSpan(
                    RubySpan(match.base, match.reading, baseStyles),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }
}
