package de.miaowz.glossa.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import de.miaowz.glossa.R
import de.miaowz.glossa.data.model.ExampleSentence
import de.miaowz.glossa.richtext.RichText
import de.miaowz.glossa.richtext.RichTextEditorState
import de.miaowz.glossa.richtext.RichTextField
import de.miaowz.glossa.ruby.RubyText

/** [key] identifies the example in the UI while examples are added and removed. */
data class ExampleDraft(
    val key: Long,
    val sentence: RichText = RichText.Empty,
    val translation: RichText = RichText.Empty,
)

data class SavedEntry(val id: Long, val wasNew: Boolean)

/** Creates an editor state and registers it, so the screen knows which field has focus. */
@Composable
fun rememberRegisteredEditor(editors: SnapshotStateList<RichTextEditorState>): RichTextEditorState {
    val editor = remember { RichTextEditorState() }
    DisposableEffect(editor) {
        editors += editor
        onDispose { editors -= editor }
    }
    return editor
}

@Composable
fun ExampleEditor(
    index: Int,
    example: ExampleDraft,
    editors: SnapshotStateList<RichTextEditorState>,
    onSentenceChange: (RichText) -> Unit,
    onTranslationChange: (RichText) -> Unit,
    onRemove: () -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.example_num, index + 1),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.remove_example_content_description, index + 1))
                }
            }
            RichTextField(
                value = example.sentence,
                onValueChange = onSentenceChange,
                state = rememberRegisteredEditor(editors),
                placeholder = stringResource(R.string.rich_text_sentence_placeholder),
                minLines = 2,
            )
            RichTextField(
                value = example.translation,
                onValueChange = onTranslationChange,
                state = rememberRegisteredEditor(editors),
                placeholder = stringResource(R.string.rich_text_translation_note_placeholder),
            )
        }
    }
}

/** Read-only "Examples" section; renders nothing if [examples] is empty. */
@Composable
fun ExamplesSection(examples: List<ExampleSentence>) {
    if (examples.isEmpty()) return
    HorizontalDivider()
    Text(
        stringResource(R.string.text_examples),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    examples.forEach { example ->
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RubyText(text = example.sentence, style = MaterialTheme.typography.bodyLarge)
                if (!example.translation.isBlank) {
                    RubyText(
                        text = example.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Non-blank drafts as [ExampleSentence]s, for previews. */
fun List<ExampleDraft>.toPreviewExamples(): List<ExampleSentence> =
    filterNot { it.sentence.isBlank && it.translation.isBlank }.map { ExampleSentence(it.sentence, it.translation) }

fun TextFieldValue.replaceSelection(replacement: String): TextFieldValue {
    val start = selection.min
    val newText = text.replaceRange(start, selection.max, replacement)
    return TextFieldValue(newText, TextRange(start + replacement.length))
}

fun TextFieldValue.selectedText(): String = text.substring(selection.min, selection.max)
