package de.miaowz.glossa.ruby

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import de.miaowz.glossa.richtext.RichText
import de.miaowz.glossa.richtext.toSpannable

/**
 * Read-only text view that renders [RichText] and draws ruby markup (`{漢字|かんじ}`)
 * as furigana above the base text.
 */
class RubyTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TextView(context, attrs) {

    private var currentValue: RichText? = null

    fun setRichText(value: RichText) {
        if (value == currentValue) return
        currentValue = value
        text = value.toSpannable().also(RubySpan::applyTo)
    }

    /** Sets unformatted text that may contain ruby markup. */
    fun setMarkupText(value: String) = setRichText(RichText(value))
}
