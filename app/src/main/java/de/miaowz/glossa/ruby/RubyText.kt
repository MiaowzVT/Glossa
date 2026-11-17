package de.miaowz.glossa.ruby

import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView
import de.miaowz.glossa.richtext.RichText

/**
 * Compose wrapper around [RubyTextView]: renders rich text with furigana. Of [style], size, weight,
 * italic, color and text alignment are applied.
 */
@Composable
fun RubyText(
    text: RichText,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    val textColor = when {
        color.isSpecified -> color
        style.color.isSpecified -> style.color
        else -> LocalContentColor.current
    }
    AndroidView(
        factory = { context -> RubyTextView(context) },
        onReset = {},
        modifier = modifier,
        update = { view ->
            view.setTextColor(textColor.toArgb())
            if (style.fontSize.isSpecified) {
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSize.value)
            }
            val bold = (style.fontWeight ?: FontWeight.Normal) >= FontWeight.SemiBold
            val italic = style.fontStyle == FontStyle.Italic
            view.setTypeface(
                Typeface.DEFAULT,
                when {
                    bold && italic -> Typeface.BOLD_ITALIC
                    bold -> Typeface.BOLD
                    italic -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                },
            )
            view.gravity = when (style.textAlign) {
                TextAlign.Center -> Gravity.CENTER_HORIZONTAL
                TextAlign.End, TextAlign.Right -> Gravity.END
                else -> Gravity.START
            }
            view.maxLines = maxLines
            view.ellipsize = if (maxLines == Int.MAX_VALUE) null else TextUtils.TruncateAt.END
            view.setRichText(text)
        },
    )
}

/** Renders plain text that may contain ruby markup, e.g. an entry title. */
@Composable
fun RubyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) = RubyText(RichText(text), modifier, style, color, maxLines)
