package de.miaowz.glossa.ruby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.miaowz.glossa.R

/**
 * Asks for the reading of [initialBase] (usually the selected text) and returns
 * both through [onConfirm]. Shows a live ruby preview.
 */
@Composable
fun FuriganaDialog(
    initialBase: String,
    onConfirm: (base: String, reading: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var base by rememberSaveable { mutableStateOf(initialBase) }
    var reading by rememberSaveable { mutableStateOf("") }
    val isValid = RubyMarkup.isValidPart(base) && RubyMarkup.isValidPart(reading)
    val readingFocus = remember { FocusRequester() }
    val baseFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (base.isEmpty()) baseFocus.requestFocus() else readingFocus.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_furigana)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = base,
                    onValueChange = { base = it },
                    label = { Text(stringResource(R.string.text)) },
                    placeholder = { Text(stringResource(R.string.kanji)) },
                    singleLine = true,
                    isError = base.isNotEmpty() && !RubyMarkup.isValidPart(base),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().focusRequester(baseFocus),
                )
                OutlinedTextField(
                    value = reading,
                    onValueChange = { reading = it },
                    label = { Text(stringResource(R.string.reading)) },
                    placeholder = { Text(stringResource(R.string.kanji_hiragana)) },
                    singleLine = true,
                    isError = reading.isNotEmpty() && !RubyMarkup.isValidPart(reading),
                    supportingText = { Text(stringResource(R.string.disallowed_characters)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (isValid) onConfirm(base, reading.trim()) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(readingFocus),
                )
                if (isValid) {
                    Text(stringResource(R.string.preview), style = MaterialTheme.typography.labelMedium)
                    RubyText(
                        text = RubyMarkup.format(base, reading.trim()),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(base, reading.trim()) }, enabled = isValid) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
