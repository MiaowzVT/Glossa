package de.miaowz.glossa.ui.common

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.miaowz.glossa.R
import de.miaowz.glossa.data.model.Category
import de.miaowz.glossa.data.model.Language

data class CategoryOptions(
    val languages: List<Language> = emptyList(),
    /** Categories of the selected language. */
    val categories: List<Category> = emptyList(),
)

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
            label = stringResource(R.string.picker_label_language),
            selectedText = options.languages.firstOrNull { it.id == languageId }?.name.orEmpty(),
            placeholder = if (options.languages.isEmpty()) stringResource(R.string.picker_placeholder_languages_empty) else stringResource(R.string.picker_placeholder_languages_filled),
            error = languageError,
            enabled = true,
            items = options.languages.map { it.id to it.name },
            noneLabel = null,
            addLabel = stringResource(R.string.picker_add_label),
            onSelect = { it?.let(onSelectLanguage) },
            onAdd = { newLanguageDialog = true },
        )
        Picker(
            label = stringResource(R.string.picker_label_category),
            selectedText = options.categories.firstOrNull { it.id == categoryId }?.name ?: stringResource(R.string.picker_category_selected_text_fallback),
            placeholder = null,
            error = null,
            enabled = languageId != null,
            items = options.categories.map { it.id to it.name },
            noneLabel = stringResource(R.string.picker_label_category_none),
            addLabel = stringResource(R.string.picker_label_category_add),
            onSelect = onSelectCategory,
            onAdd = { newCategoryDialog = true },
        )
    }

    if (newLanguageDialog) {
        NameDialog(
            title = stringResource(R.string.name_dialog_title_new_language),
            label = stringResource(R.string.name_dialog_label_new_language),
            placeholder = stringResource(R.string.name_dialog_placeholder_new_language),
            onConfirm = {
                newLanguageDialog = false
                onCreateLanguage(it)
            },
            onDismiss = { newLanguageDialog = false },
        )
    }
    if (newCategoryDialog) {
        NameDialog(
            title = stringResource(R.string.name_dialog_title_new_category),
            label = stringResource(R.string.name_dialog_label_new_category),
            placeholder = stringResource(R.string.name_dialog_placeholder_new_category),
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
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.name_dialog_confirm_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.name_dialog_dismiss_button)) }
        },
    )
}
