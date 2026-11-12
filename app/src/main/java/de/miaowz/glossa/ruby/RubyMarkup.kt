package de.miaowz.glossa.ruby

/**
 * Inline ruby (furigana) markup: `{base|reading}`, e.g. `{漢字|かんじ}`.
 *
 * Neither part may contain `{`, `}` or `|`; the base and reading must not be blank.
 */
object RubyMarkup {
    private val pattern = Regex("""\{([^{}|]+)\|([^{}|]+)\}""")

    data class Match(
        /** Range of the whole markup, `{` through `}`, in the source text. */
        val range: IntRange,
        val base: String,
        val reading: String,
    ) {
        /** Offset of the base text in the source text. */
        val baseStart: Int get() = range.first + 1
        val baseEnd: Int get() = baseStart + base.length
    }

    fun findAll(text: CharSequence): List<Match> = pattern.findAll(text)
        .filter { it.groupValues[1].isNotBlank() && it.groupValues[2].isNotBlank() }
        .map { Match(it.range, it.groupValues[1], it.groupValues[2]) }
        .toList()

    fun format(base: String, reading: String): String = "{$base|$reading}"

    /** Whether [value] can be used as base or reading without breaking the markup. */
    fun isValidPart(value: String): Boolean =
        value.isNotBlank() && value.none { it == '{' || it == '}' || it == '|' }

    /** Replaces markup with its base text, e.g. for search and plain-text previews. */
    fun stripToBase(text: String): String = pattern.replace(text) { match ->
        if (match.groupValues[1].isBlank() || match.groupValues[2].isBlank()) match.value
        else match.groupValues[1]
    }
}
