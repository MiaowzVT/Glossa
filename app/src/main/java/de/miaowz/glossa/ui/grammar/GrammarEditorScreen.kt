package de.miaowz.glossa.ui.grammar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.miaowz.glossa.R
import de.miaowz.glossa.richtext.RichTextEditorState
import de.miaowz.glossa.richtext.RichTextField
import de.miaowz.glossa.richtext.RichTextToolbar
import de.miaowz.glossa.ruby.FuriganaDialog
import de.miaowz.glossa.ruby.RubyMarkup
import de.miaowz.glossa.ruby.RubyText
import de.miaowz.glossa.ui.common.CategoryPickers
import de.miaowz.glossa.ui.common.ExampleEditor
import de.miaowz.glossa.ui.common.ExamplesSection
import de.miaowz.glossa.ui.common.rememberRegisteredEditor
import de.miaowz.glossa.ui.common.replaceSelection
import de.miaowz.glossa.ui.common.selectedText
import de.miaowz.glossa.ui.common.toPreviewExamples
import de.miaowz.glossa.ui.grammarRepository

/** Where the furigana dialog inserts its result. */
private sealed interface FuriganaTarget {
    data class Rich(val state: RichTextEditorState) : FuriganaTarget
    data object Title : FuriganaTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarEditorScreen(
    onClose: () -> Unit,
    onSaved: (id: Long, wasNew: Boolean) -> Unit,
    viewModel: GrammarEditorViewModel = viewModel {
        GrammarEditorViewModel(createSavedStateHandle(), grammarRepository)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val options by viewModel.categoryOptions.collectAsStateWithLifecycle()
    var preview by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var furiganaTarget by remember { mutableStateOf<FuriganaTarget?>(null) }
    val editors = remember { mutableStateListOf<RichTextEditorState>() }
    val focusedEditor = editors.firstOrNull { it.isFocused }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val dismissKeyboard = {
        focusedEditor?.clearFocus()
        focusManager.clearFocus()
        keyboard?.hide()
    }
    val close = {
        dismissKeyboard()
        onClose()
    }

    LaunchedEffect(state.saved) {
        state.saved?.let {
            dismissKeyboard()
            onSaved(it.id, it.wasNew)
        }
    }
    BackHandler(enabled = state.isDirty) { confirmDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) stringResource(R.string.new_entry) else stringResource(R.string.edit_entry)) },
                navigationIcon = {
                    IconButton(onClick = { if (state.isDirty) confirmDiscard = true else close() }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        preview = !preview
                        if (preview) dismissKeyboard()
                    }) {
                        if (preview) Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.close))
                        else Icon(Icons.Filled.Visibility, contentDescription = stringResource(R.string.preview))
                    }
                    TextButton(onClick = viewModel::save, enabled = !state.isLoading && !state.isSaving) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
        bottomBar = {
            if (focusedEditor != null && !preview) {
                RichTextToolbar(
                    state = focusedEditor,
                    onFuriganaClick = { furiganaTarget = FuriganaTarget.Rich(focusedEditor) },
                    modifier = Modifier.navigationBarsPadding().imePadding(),
                )
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (preview) {
                EntryPreview(state)
            } else {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::setTitle,
                    label = { Text(stringResource(R.string.title)) },
                    placeholder = { Text(stringResource(R.string.title_example_placeholder)) },
                    isError = state.titleError != null,
                    supportingText = state.titleError?.let { { Text(stringResource(it)) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    trailingIcon = {
                        IconButton(onClick = { furiganaTarget = FuriganaTarget.Title }) {
                            Text(stringResource(R.string.furigana_button_title), fontSize = 18.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                CategoryPickers(
                    options = options,
                    languageId = state.languageId,
                    categoryId = state.categoryId,
                    languageError = state.languageError?.let { stringResource(it) },
                    onSelectLanguage = viewModel::selectLanguage,
                    onSelectCategory = viewModel::selectCategory,
                    onCreateLanguage = viewModel::createLanguage,
                    onCreateCategory = viewModel::createCategory,
                )
                RichTextField(
                    value = state.description,
                    onValueChange = viewModel::setDescription,
                    state = rememberRegisteredEditor(editors),
                    label = stringResource(R.string.description),
                    placeholder = stringResource(R.string.grammar_description_placeholder),
                    minLines = 5,
                )
                Text(
                    stringResource(R.string.furigana_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                Text(stringResource(R.string.example_sentences), style = MaterialTheme.typography.titleMedium)
                state.examples.forEachIndexed { index, example ->
                    key(example.key) {
                        ExampleEditor(
                            index = index,
                            example = example,
                            editors = editors,
                            onSentenceChange = { value ->
                                viewModel.updateExample(example.key) { it.copy(sentence = value) }
                            },
                            onTranslationChange = { value ->
                                viewModel.updateExample(example.key) { it.copy(translation = value) }
                            },
                            onRemove = { viewModel.removeExample(example.key) },
                        )
                    }
                }
                OutlinedButton(onClick = viewModel::addExample, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.add_example), Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    when (val target = furiganaTarget) {
        null -> Unit
        is FuriganaTarget.Rich -> FuriganaDialog(
            initialBase = target.state.formatState.selectedText,
            onConfirm = { base, reading ->
                target.state.insertRuby(base, reading)
                furiganaTarget = null
            },
            onDismiss = { furiganaTarget = null },
        )
        FuriganaTarget.Title -> FuriganaDialog(
            initialBase = state.title.selectedText(),
            onConfirm = { base, reading ->
                viewModel.setTitle(state.title.replaceSelection(RubyMarkup.format(base, reading)))
                furiganaTarget = null
            },
            onDismiss = { furiganaTarget = null },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_changes_confirmation)) },
            text = { Text(stringResource(R.string.discard_changes_confirmation_detail)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    close()
                }) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.keep_editing)) }
            },
        )
    }
}

@Composable
private fun EntryPreview(state: EditorUiState) {
    if (state.title.text.isBlank()) {
        Text(stringResource(R.string.untitled), style = MaterialTheme.typography.headlineMedium)
    } else {
        RubyText(text = state.title.text, style = MaterialTheme.typography.headlineMedium)
    }
    if (!state.description.isBlank) {
        RubyText(text = state.description, style = MaterialTheme.typography.bodyLarge)
    }
    ExamplesSection(state.examples.toPreviewExamples())
}
