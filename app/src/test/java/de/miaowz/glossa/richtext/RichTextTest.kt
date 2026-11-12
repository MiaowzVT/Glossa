package de.miaowz.glossa.richtext

import org.junit.Assert.assertEquals
import org.junit.Test

class RichTextTest {

    @Test
    fun encodeDecodeRoundTripKeepsAllFormats() {
        val value = RichText(
            text = "{漢字|かんじ} is bold and red",
            spans = listOf(
                RichSpan(0, 9, SpanFormat.Bold),
                RichSpan(1, 3, SpanFormat.Italic),
                RichSpan(10, 12, SpanFormat.Underline),
                RichSpan(13, 17, SpanFormat.Strikethrough),
                RichSpan(21, 24, SpanFormat.TextColor(0xFFE53935.toInt())),
                RichSpan(18, 21, SpanFormat.Highlight(0x80FFEB3B.toInt())),
                RichSpan(0, 2, SpanFormat.RelativeSize(1.3f)),
            ),
        )
        assertEquals(value, RichText.decode(value.encode()))
    }

    @Test
    fun decodeFallsBackToPlainTextForNonJson() {
        assertEquals(RichText("just text"), RichText.decode("just text"))
        assertEquals(RichText.Empty, RichText.decode(""))
    }

    @Test
    fun decodeDropsInvalidSpans() {
        val encoded = RichText(
            "abc",
            listOf(
                RichSpan(0, 2, SpanFormat.Bold),
                RichSpan(2, 2, SpanFormat.Italic),
                RichSpan(1, 10, SpanFormat.Underline),
            ),
        ).encode()

        assertEquals(listOf(RichSpan(0, 2, SpanFormat.Bold)), RichText.decode(encoded).spans)
    }
}
