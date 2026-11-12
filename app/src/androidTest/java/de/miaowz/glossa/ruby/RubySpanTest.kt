package de.miaowz.glossa.ruby

import android.graphics.Paint
import android.text.style.CharacterStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.miaowz.glossa.richtext.RichSpan
import de.miaowz.glossa.richtext.RichText
import de.miaowz.glossa.richtext.SpanFormat
import de.miaowz.glossa.richtext.toSpannable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RubySpanTest {

    @Test
    fun replacesMarkupWithRubySpans() {
        val text = RichText("{今日|きょう}は{雨|あめ}").toSpannable()
        RubySpan.applyTo(text)

        val spans = text.getSpans(0, text.length, RubySpan::class.java).sortedBy { text.getSpanStart(it) }
        assertEquals(listOf("今日" to "きょう", "雨" to "あめ"), spans.map { it.base to it.reading })
        assertEquals(0, text.getSpanStart(spans[0]))
        assertEquals(8, text.getSpanEnd(spans[0]))
    }

    @Test
    fun cutsStylesOutOfMarkupAndKeepsTheRest() {
        // Bold covers "a{漢字|かんじ}b", so it must be split around the ruby span.
        val text = RichText("a{漢字|かんじ}b", listOf(RichSpan(0, 10, SpanFormat.Bold))).toSpannable()
        RubySpan.applyTo(text)

        val ruby = text.getSpans(0, text.length, RubySpan::class.java).single()
        val rubyStart = text.getSpanStart(ruby)
        val rubyEnd = text.getSpanEnd(ruby)
        val styles = text.getSpans(0, text.length, CharacterStyle::class.java).filter { it !is RubySpan }
        assertEquals(2, styles.size)
        assertTrue(styles.none { text.getSpanStart(it) < rubyEnd && text.getSpanEnd(it) > rubyStart })
    }

    @Test
    fun rubyMakesLineTallerAndAtLeastAsWideAsReading() {
        val span = RubySpan("日", "にち")
        val paint = Paint().apply { textSize = 40f }
        val metrics = Paint.FontMetricsInt()
        val width = span.getSize(paint, "{日|にち}", 0, 6, metrics)

        val plain = paint.fontMetricsInt
        assertTrue(metrics.ascent < plain.ascent)
        assertTrue(width >= Paint().apply { textSize = 20f }.measureText("にち").toInt())
    }
}
