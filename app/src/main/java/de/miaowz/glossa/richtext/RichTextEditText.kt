package de.miaowz.glossa.richtext

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import de.miaowz.glossa.ruby.RubyMarkup

/** Formatting state of the current selection, used to highlight toolbar buttons. */
data class FormatState(
    val formats: Map<FormatKind, SpanFormat> = emptyMap(),
    val selectedText: String = "",
) {
    fun isActive(format: SpanFormat): Boolean = formats[format.kind] == format
}

/**
 * Multi-line [EditText] with span-based formatting. Its content is exchanged as [RichText].
 *
 * With a collapsed cursor, formatting commands set "typing formats" that are applied to the
 * text typed next, like in word processors.
 */
class RichTextEditText(context: Context) : EditText(context) {

    var onRichTextChange: ((RichText) -> Unit)? = null
    var onFormatStateChange: ((FormatState) -> Unit)? = null

    private var typingFormats: Map<FormatKind, SpanFormat> = emptyMap()
    private var isEditingProgrammatically = false
    private var lastRichText = RichText.Empty
    private var appliedColors: Triple<Int, Int, Int>? = null

    init {
        inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        gravity = Gravity.TOP or Gravity.START
        setHorizontallyScrolling(false)
        isVerticalScrollBarEnabled = false
        background = null
        addTextChangedListener(FormattingWatcher())
    }

    val richText: RichText get() = lastRichText

    /** Replaces the content, unless it already equals [value] (keeps cursor and IME state). */
    fun setRichText(value: RichText) {
        if (value == lastRichText) return
        editProgrammatically { setText(value.toSpannable()) }
        lastRichText = value
        setSelection(value.text.length)
    }

    /** Applies theme colors; a no-op if they are unchanged, to avoid needless relayouts. */
    fun applyColors(text: Int, hint: Int, accent: Int) {
        val colors = Triple(text, hint, accent)
        if (colors == appliedColors) return
        appliedColors = colors
        setTextColor(text)
        setHintTextColor(hint)
        highlightColor = (accent and 0x00FFFFFF) or 0x40000000
        textCursorDrawable = GradientDrawable().apply {
            setColor(accent)
            setSize(dp(2), 0)
        }
        val tint = ColorStateList.valueOf(accent)
        textSelectHandle?.setTintList(tint)
        textSelectHandleLeft?.setTintList(tint)
        textSelectHandleRight?.setTintList(tint)
    }

    /** Toggles an on/off format (bold, italic, underline, strikethrough). */
    fun toggle(format: SpanFormat) {
        val state = currentFormats()
        setFormat(format.kind, if (state[format.kind] == format) null else format)
    }

    /** Sets [format] for [kind] on the selection, or removes it when null. */
    fun setFormat(kind: FormatKind, format: SpanFormat?) {
        val text = text ?: return
        val start = selectionStart
        val end = selectionEnd
        if (start == end) {
            finishComposing()
            typingFormats = if (format == null) typingFormats - kind else typingFormats + (kind to format)
        } else {
            editProgrammatically {
                if (format == null) SpanOps.remove(text, kind, start, end)
                else SpanOps.apply(text, format, start, end)
            }
        }
        notifyChanged()
    }

    fun clearFormatting() {
        val text = text ?: return
        if (selectionStart == selectionEnd) {
            typingFormats = emptyMap()
        } else {
            editProgrammatically { SpanOps.clear(text, selectionStart, selectionEnd) }
        }
        notifyChanged()
    }

    /**
     * Inserts furigana markup `{base|reading}` in place of the selection. If [base] is the
     * selected text, the selection is wrapped instead, which keeps its formatting.
     */
    fun insertRuby(base: String, reading: String) {
        val text = text ?: return
        finishComposing()
        val start = selectionStart
        val end = selectionEnd
        editProgrammatically {
            if (start != end && text.substring(start, end) == base) {
                text.insert(end, "|$reading}")
                text.insert(start, "{")
            } else {
                text.replace(start, end, RubyMarkup.format(base, reading))
            }
        }
        Selection.setSelection(text, start + RubyMarkup.format(base, reading).length)
        notifyChanged()
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        // Called from the super constructor, before the fields are initialized.
        @Suppress("SENSELESS_COMPARISON")
        if (typingFormats == null) return
        val text = text ?: return
        typingFormats = SpanOps.formatsAt(text, selStart)
        notifyFormatState()
    }

    override fun onTextContextMenuItem(id: Int): Boolean =
        // Foreign spans would not be saved anyway; pasting plain text keeps what you see honest.
        super.onTextContextMenuItem(if (id == android.R.id.paste) android.R.id.pasteAsPlainText else id)

    private fun currentFormats(): Map<FormatKind, SpanFormat> {
        val text = text ?: return emptyMap()
        return if (selectionStart == selectionEnd) typingFormats
        else SpanOps.formatsIn(text, selectionStart, selectionEnd)
    }

    private fun notifyChanged() {
        notifyFormatState()
        val value = text?.toRichText() ?: RichText.Empty
        if (value != lastRichText) {
            lastRichText = value
            onRichTextChange?.invoke(value)
        }
    }

    private fun notifyFormatState() {
        val text = text ?: return
        val start = selectionStart.coerceAtLeast(0)
        val end = selectionEnd.coerceAtLeast(0)
        onFormatStateChange?.invoke(
            FormatState(currentFormats(), text.substring(minOf(start, end), maxOf(start, end)))
        )
    }

    private inline fun editProgrammatically(block: () -> Unit) {
        isEditingProgrammatically = true
        try {
            block()
        } finally {
            isEditingProgrammatically = false
        }
    }

    /** Commits pending IME composition, so a format change does not restyle the composed word. */
    private fun finishComposing() {
        val text = text ?: return
        if (BaseInputConnection.getComposingSpanStart(text) != -1) {
            context.getSystemService(InputMethodManager::class.java)?.restartInput(this)
        }
    }

    private inner class FormattingWatcher : TextWatcher {
        private var insertedStart = 0
        private var insertedEnd = 0

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            insertedStart = start
            insertedEnd = start + count
        }

        override fun afterTextChanged(s: Editable) {
            if (isEditingProgrammatically) return
            if (insertedEnd > insertedStart) {
                // Make typed text follow the typing formats, not whatever spans it landed in.
                editProgrammatically {
                    for (kind in FormatKind.entries) {
                        val format = typingFormats[kind]
                        if (format != null) SpanOps.apply(s, format, insertedStart, insertedEnd)
                        else SpanOps.remove(s, kind, insertedStart, insertedEnd)
                    }
                }
            }
            notifyChanged()
        }
    }
}
