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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

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
        title = { Text("Add furigana") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = base,
                    onValueChange = { base = it },
                    label = { Text("Text") },
                    placeholder = { Text("漢字") },
                    singleLine = true,
                    isError = base.isNotEmpty() && !RubyMarkup.isValidPart(base),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().focusRequester(baseFocus),
                )
                OutlinedTextField(
                    value = reading,
                    onValueChange = { reading = it },
                    label = { Text("Reading") },
                    placeholder = { Text("かんじ") },
                    singleLine = true,
                    isError = reading.isNotEmpty() && !RubyMarkup.isValidPart(reading),
                    supportingText = { Text("The characters { } | are not allowed") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (isValid) onConfirm(base, reading.trim()) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(readingFocus),
                )
                if (isValid) {
                    Text("Preview", style = MaterialTheme.typography.labelMedium)
                    RubyText(
                        text = RubyMarkup.format(base, reading.trim()),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(base, reading.trim()) }, enabled = isValid) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
