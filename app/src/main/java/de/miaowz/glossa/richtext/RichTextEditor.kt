package de.miaowz.glossa.richtext

import android.util.TypedValue
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Handle to a [RichTextField], used by [RichTextToolbar] to format the focused field.
 */
@Stable
class RichTextEditorState {
    internal var view: RichTextEditText? = null

    var formatState by mutableStateOf(FormatState())
        internal set

    var isFocused by mutableStateOf(false)
        internal set

    fun toggle(format: SpanFormat) {
        view?.toggle(format)
    }

    fun setFormat(kind: FormatKind, format: SpanFormat?) {
        view?.setFormat(kind, format)
    }

    fun clearFormatting() {
        view?.clearFormatting()
    }

    fun insertRuby(base: String, reading: String) {
        view?.insertRuby(base, reading)
    }

    fun clearFocus() {
        view?.clearFocus()
    }
}

@Composable
fun rememberRichTextEditorState(): RichTextEditorState = remember { RichTextEditorState() }

/** Outlined, multi-line rich text input. Formatting is applied through [state]. */
@Composable
fun RichTextField(
    value: RichText,
    onValueChange: (RichText) -> Unit,
    state: RichTextEditorState,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    minLines: Int = 1,
) {
    val colors = MaterialTheme.colorScheme
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val borderColor = if (state.isFocused) colors.primary else colors.outline
    val textColor = colors.onSurface.toArgb()
    val hintColor = colors.onSurfaceVariant.toArgb()
    val accentColor = colors.primary.toArgb()

    Column(modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (state.isFocused) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        }
        AndroidView(
            factory = { context ->
                RichTextEditText(context).apply {
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setLineSpacing(0f, 1.15f)
                    state.view = this
                    onRichTextChange = { currentOnValueChange(it) }
                    onFormatStateChange = { state.formatState = it }
                    setOnFocusChangeListener { _, hasFocus -> state.isFocused = hasFocus }
                }
            },
            update = { view ->
                // Only touch properties that changed: e.g. setHint() relayouts the view,
                // which ends the selection action mode and collapses the selection.
                view.setRichText(value)
                view.applyColors(textColor, hintColor, accentColor)
                if (view.hint?.toString() != placeholder) view.hint = placeholder
                if (view.minLines != minLines) view.minLines = minLines
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (state.isFocused) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(4.dp),
                ),
        )
    }

    DisposableEffect(state) {
        onDispose {
            state.view = null
            state.isFocused = false
        }
    }
}

internal fun android.view.View.dp(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()
