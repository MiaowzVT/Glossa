package de.miaowz.glossa.richtext

import android.text.SpannableStringBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpanOpsTest {

    private fun spannable(text: String, vararg spans: RichSpan) =
        RichText(text, spans.toList()).toSpannable()

    private fun SpannableStringBuilder.spans() = toRichText().spans

    @Test
    fun applyMergesAdjacentSpansOfSameFormat() {
        val text = spannable("abcdef", RichSpan(0, 2, SpanFormat.Bold))
        SpanOps.apply(text, SpanFormat.Bold, 2, 4)
        assertEquals(listOf(RichSpan(0, 4, SpanFormat.Bold)), text.spans())
    }

    @Test
    fun applyReplacesOtherValuesOfSameKind() {
        val red = SpanFormat.TextColor(0xFFFF0000.toInt())
        val blue = SpanFormat.TextColor(0xFF0000FF.toInt())
        val text = spannable("abcdef", RichSpan(0, 6, red))
        SpanOps.apply(text, blue, 2, 4)
        assertEquals(
            listOf(RichSpan(0, 2, red), RichSpan(2, 4, blue), RichSpan(4, 6, red)),
            text.spans(),
        )
    }

    @Test
    fun removeSplitsSpan() {
        val text = spannable("abcdef", RichSpan(0, 6, SpanFormat.Italic), RichSpan(0, 6, SpanFormat.Bold))
        SpanOps.remove(text, FormatKind.Italic, 2, 4)
        assertEquals(
            listOf(
                RichSpan(0, 2, SpanFormat.Italic),
                RichSpan(0, 6, SpanFormat.Bold),
                RichSpan(4, 6, SpanFormat.Italic),
            ),
            text.spans(),
        )
    }

    @Test
    fun formatsInRequiresFullCoverage() {
        val text = spannable("abcdef", RichSpan(0, 3, SpanFormat.Bold), RichSpan(3, 6, SpanFormat.Bold))
        assertEquals(SpanFormat.Bold, SpanOps.formatsIn(text, 1, 5)[FormatKind.Bold])

        val partial = spannable("abcdef", RichSpan(0, 3, SpanFormat.Bold))
        assertTrue(FormatKind.Bold !in SpanOps.formatsIn(partial, 1, 5))
    }

    @Test
    fun formatsInIgnoresSpansOnlyTouchingTheRange() {
        val red = SpanFormat.TextColor(0xFFFF0000.toInt())
        val blue = SpanFormat.TextColor(0xFF0000FF.toInt())
        val text = spannable("abcdef", RichSpan(0, 2, red), RichSpan(2, 4, blue))
        assertEquals(blue, SpanOps.formatsIn(text, 2, 4)[FormatKind.TextColor])
    }

    @Test
    fun formatsAtUsesPrecedingCharacter() {
        val text = spannable("abcdef", RichSpan(0, 3, SpanFormat.Bold))
        assertEquals(SpanFormat.Bold, SpanOps.formatsAt(text, 3)[FormatKind.Bold])
        assertTrue(SpanOps.formatsAt(text, 4).isEmpty())
    }

    @Test
    fun toRichTextIgnoresForeignSpans() {
        val text = spannable("abc")
        text.setSpan(android.text.style.UnderlineSpan(), 0, 3, 0)
        assertTrue(text.spans().isEmpty())
    }
}
