package de.miaowz.glossa.richtext

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colors are mid-tones so they stay readable on light and dark backgrounds. */
val TextColorPalette = listOf(
    0xFFE53935, 0xFFFB8C00, 0xFF43A047, 0xFF00897B,
    0xFF1E88E5, 0xFF8E24AA, 0xFFD81B60, 0xFF757575,
).map { it.toInt() }

/** Translucent so the text color underneath still applies in both themes. */
val HighlightPalette = listOf(
    0x80FFEB3B, 0x80FFA726, 0x8066BB6A, 0x8042A5F5, 0x80AB47BC, 0x80F06292,
).map { it.toInt() }

private val SizeOptions = listOf(
    "Small" to 0.8f,
    "Normal" to null,
    "Large" to 1.3f,
    "Huge" to 1.6f,
)

private enum class Panel { None, TextColor, Highlight, Size }

/**
 * Formatting toolbar for the [RichTextField] attached to [state].
 * [onFuriganaClick] should open a [FuriganaDialog] and call [RichTextEditorState.insertRuby].
 */
@Composable
fun RichTextToolbar(
    state: RichTextEditorState,
    onFuriganaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var panel by rememberSaveable { mutableStateOf(Panel.None) }
    val formatState = state.formatState

    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 3.dp, shadowElevation = 3.dp) {
        Column {
            AnimatedVisibility(visible = panel != Panel.None) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (panel) {
                        Panel.TextColor -> ColorPanel(
                            colors = TextColorPalette,
                            selected = (formatState.formats[FormatKind.TextColor] as? SpanFormat.TextColor)?.argb,
                            onSelect = { argb ->
                                state.setFormat(FormatKind.TextColor, argb?.let { SpanFormat.TextColor(it) })
                            },
                        )
                        Panel.Highlight -> ColorPanel(
                            colors = HighlightPalette,
                            selected = (formatState.formats[FormatKind.Highlight] as? SpanFormat.Highlight)?.argb,
                            onSelect = { argb ->
                                state.setFormat(FormatKind.Highlight, argb?.let { SpanFormat.Highlight(it) })
                            },
                        )
                        Panel.Size -> {
                            val current = (formatState.formats[FormatKind.RelativeSize] as? SpanFormat.RelativeSize)
                                ?.proportion
                            SizeOptions.forEach { (label, proportion) ->
                                FilterChip(
                                    selected = current == proportion,
                                    onClick = {
                                        state.setFormat(
                                            FormatKind.RelativeSize,
                                            proportion?.let { SpanFormat.RelativeSize(it) },
                                        )
                                    },
                                    label = { Text(label) },
                                )
                            }
                        }
                        Panel.None -> Unit
                    }
                }
            }
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToggleFormatButton(state, SpanFormat.Bold, Icons.Filled.FormatBold, "Bold")
                ToggleFormatButton(state, SpanFormat.Italic, Icons.Filled.FormatItalic, "Italic")
                ToggleFormatButton(state, SpanFormat.Underline, Icons.Filled.FormatUnderlined, "Underline")
                ToggleFormatButton(
                    state, SpanFormat.Strikethrough, Icons.Filled.FormatStrikethrough, "Strikethrough",
                )
                VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))
                PanelButton(
                    icon = Icons.Filled.FormatColorText,
                    description = "Text color",
                    indicator = (formatState.formats[FormatKind.TextColor] as? SpanFormat.TextColor)
                        ?.let { Color(it.argb) },
                    checked = panel == Panel.TextColor,
                    onClick = { panel = if (panel == Panel.TextColor) Panel.None else Panel.TextColor },
                )
                PanelButton(
                    icon = Icons.Filled.FormatColorFill,
                    description = "Highlight",
                    indicator = (formatState.formats[FormatKind.Highlight] as? SpanFormat.Highlight)
                        ?.let { Color(it.argb) },
                    checked = panel == Panel.Highlight,
                    onClick = { panel = if (panel == Panel.Highlight) Panel.None else Panel.Highlight },
                )
                PanelButton(
                    icon = Icons.Filled.FormatSize,
                    description = "Text size",
                    indicator = null,
                    checked = panel == Panel.Size,
                    onClick = { panel = if (panel == Panel.Size) Panel.None else Panel.Size },
                )
                VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))
                IconButton(
                    onClick = onFuriganaClick,
                    modifier = Modifier.semantics { contentDescription = "Add furigana" },
                ) {
                    Text("ふ", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { state.clearFormatting() }) {
                    Icon(Icons.Filled.FormatClear, contentDescription = "Clear formatting")
                }
            }
        }
    }
}

@Composable
private fun ToggleFormatButton(
    state: RichTextEditorState,
    format: SpanFormat,
    icon: ImageVector,
    description: String,
) {
    IconToggleButton(
        checked = state.formatState.isActive(format),
        onCheckedChange = { state.toggle(format) },
    ) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun PanelButton(
    icon: ImageVector,
    description: String,
    indicator: Color?,
    checked: Boolean,
    onClick: () -> Unit,
) {
    IconToggleButton(checked = checked, onCheckedChange = { onClick() }) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Icon(icon, contentDescription = description, modifier = Modifier.padding(bottom = 4.dp))
            Box(
                Modifier
                    .width(20.dp)
                    .height(3.dp)
                    .background(indicator ?: LocalContentColor.current.copy(alpha = 0.3f), RoundedCornerShape(1.dp)),
            )
        }
    }
}

@Composable
private fun ColorPanel(colors: List<Int>, selected: Int?, onSelect: (Int?) -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable { onSelect(null) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.FormatColorReset, contentDescription = "No color", Modifier.size(18.dp))
    }
    colors.forEach { argb ->
        val isSelected = argb == selected
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                )
                .padding(if (isSelected) 5.dp else 3.dp)
                .background(Color(argb), CircleShape)
                .clickable { onSelect(argb) }
                .semantics { contentDescription = "Color #%08X".format(argb) },
        )
    }
}
