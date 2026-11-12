package de.miaowz.glossa.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/** Language and sub-category selection, with inline creation of new ones. */
@Composable
fun CategoryPickers(
    options: CategoryOptions,
    languageId: Long?,
    categoryId: Long?,
    languageError: String?,
    onSelectLanguage: (Long) -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onCreateLanguage: (String) -> Unit,
    onCreateCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var newCategoryDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Picker(
            label = "Language",
            selectedText = options.languages.firstOrNull { it.id == languageId }?.name.orEmpty(),
            placeholder = if (options.languages.isEmpty()) "Add your first language" else "Choose a language",
            error = languageError,
            enabled = true,
            items = options.languages.map { it.id to it.name },
            noneLabel = null,
            addLabel = "New language…",
            onSelect = { it?.let(onSelectLanguage) },
            onAdd = { newLanguageDialog = true },
        )
        Picker(
            label = "Category",
            selectedText = options.categories.firstOrNull { it.id == categoryId }?.name ?: "None",
            placeholder = null,
            error = null,
            enabled = languageId != null,
            items = options.categories.map { it.id to it.name },
            noneLabel = "None",
            addLabel = "New category…",
            onSelect = onSelectCategory,
            onAdd = { newCategoryDialog = true },
        )
    }

    if (newLanguageDialog) {
        NameDialog(
            title = "New language",
            label = "Language",
            placeholder = "e.g. Japanese",
            onConfirm = {
                newLanguageDialog = false
                onCreateLanguage(it)
            },
            onDismiss = { newLanguageDialog = false },
        )
    }
    if (newCategoryDialog) {
        NameDialog(
            title = "New category",
            label = "Category",
            placeholder = "e.g. Particles",
            onConfirm = {
                newCategoryDialog = false
                onCreateCategory(it)
            },
            onDismiss = { newCategoryDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Picker(
    label: String,
    selectedText: String,
    placeholder: String?,
    error: String?,
    enabled: Boolean,
    items: List<Pair<Long, String>>,
    noneLabel: String?,
    addLabel: String,
    onSelect: (Long?) -> Unit,
    onAdd: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            if (noneLabel != null) {
                DropdownMenuItem(
                    text = { Text(noneLabel) },
                    onClick = {
                        expanded = false
                        onSelect(null)
                    },
                )
            }
            items.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        expanded = false
                        onSelect(id)
                    },
                )
            }
            if (items.isNotEmpty() || noneLabel != null) HorizontalDivider()
            DropdownMenuItem(
                text = { Text(addLabel) },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAdd()
                },
            )
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    placeholder: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
