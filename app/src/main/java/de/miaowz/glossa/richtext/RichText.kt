package de.miaowz.glossa.richtext

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Platform-independent rich text: plain [text] plus formatting [spans] over character ranges.
 *
 * This is the storage format for every formatted field in the app. It is serialized as JSON
 * (see [encode]/[decode]) instead of HTML, because Android's Html round-trip is lossy
 * (it adds paragraph breaks and drops unsupported styles).
 *
 * Furigana is not a span: it is written inline as ruby markup, e.g. `{漢字|かんじ}`
 * (see [de.miaowz.glossa.ruby.RubyMarkup]), so it survives copy/paste and plain-text editing.
 */
@Serializable
data class RichText(
    val text: String = "",
    val spans: List<RichSpan> = emptyList(),
) {
    val isBlank: Boolean get() = text.isBlank()

    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        val Empty = RichText()

        private val json = Json { ignoreUnknownKeys = true }

        /** Decodes a value produced by [encode]. Falls back to plain text for legacy/invalid data. */
        fun decode(encoded: String): RichText {
            if (encoded.isEmpty()) return Empty
            return runCatching { json.decodeFromString(serializer(), encoded) }
                .getOrElse { RichText(encoded) }
                .sanitized()
        }
    }

    /** Drops spans that are empty or out of bounds, e.g. after manual database edits. */
    private fun sanitized(): RichText {
        val valid = spans.filter { it.start in 0 until it.end && it.end <= text.length }
        return if (valid.size == spans.size) this else copy(spans = valid)
    }
}

@Serializable
data class RichSpan(
    val start: Int,
    val end: Int,
    val format: SpanFormat,
)

/** Formats supported by the rich text editor. */
@Serializable
sealed interface SpanFormat {
    @Serializable @SerialName("bold")
    data object Bold : SpanFormat

    @Serializable @SerialName("italic")
    data object Italic : SpanFormat

    @Serializable @SerialName("underline")
    data object Underline : SpanFormat

    @Serializable @SerialName("strikethrough")
    data object Strikethrough : SpanFormat

    /** [argb] is a packed color as used by `android.graphics.Color`. */
    @Serializable @SerialName("color")
    data class TextColor(val argb: Int) : SpanFormat

    @Serializable @SerialName("highlight")
    data class Highlight(val argb: Int) : SpanFormat

    /** Size relative to the surrounding text, e.g. 1.5f for large text. */
    @Serializable @SerialName("size")
    data class RelativeSize(val proportion: Float) : SpanFormat
}

/** Identifies a format "slot": two formats of the same kind cannot apply to the same character. */
enum class FormatKind { Bold, Italic, Underline, Strikethrough, TextColor, Highlight, RelativeSize }

val SpanFormat.kind: FormatKind
    get() = when (this) {
        SpanFormat.Bold -> FormatKind.Bold
        SpanFormat.Italic -> FormatKind.Italic
        SpanFormat.Underline -> FormatKind.Underline
        SpanFormat.Strikethrough -> FormatKind.Strikethrough
        is SpanFormat.TextColor -> FormatKind.TextColor
        is SpanFormat.Highlight -> FormatKind.Highlight
        is SpanFormat.RelativeSize -> FormatKind.RelativeSize
    }
