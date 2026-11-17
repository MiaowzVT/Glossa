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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
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
                title = { Text(if (state.isNew) "New entry" else "Edit entry") },
                navigationIcon = {
                    IconButton(onClick = { if (state.isDirty) confirmDiscard = true else close() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        preview = !preview
                        if (preview) dismissKeyboard()
                    }) {
                        if (preview) Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        else Icon(Icons.Filled.Visibility, contentDescription = "Preview")
                    }
                    TextButton(onClick = viewModel::save, enabled = !state.isLoading && !state.isSaving) {
                        Text("Save")
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
                    label = { Text("Title") },
                    placeholder = { Text("e.g. 〜{間|あいだ}に") },
                    isError = state.titleError != null,
                    supportingText = state.titleError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    trailingIcon = {
                        IconButton(onClick = { furiganaTarget = FuriganaTarget.Title }) {
                            Text("ふ", fontSize = 18.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                CategoryPickers(
                    options = options,
                    languageId = state.languageId,
                    categoryId = state.categoryId,
                    languageError = state.languageError,
                    onSelectLanguage = viewModel::selectLanguage,
                    onSelectCategory = viewModel::selectCategory,
                    onCreateLanguage = viewModel::createLanguage,
                    onCreateCategory = viewModel::createCategory,
                )
                RichTextField(
                    value = state.description,
                    onValueChange = viewModel::setDescription,
                    state = rememberRegisteredEditor(editors),
                    label = "Description",
                    placeholder = "Explain the grammar point…",
                    minLines = 5,
                )
                Text(
                    "Tip: select text and tap ふ to add furigana, or type {漢字|かんじ}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                Text("Example sentences", style = MaterialTheme.typography.titleMedium)
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
                    Text("Add example", Modifier.padding(start = 8.dp))
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
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved changes to this entry will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    close()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") }
            },
        )
    }
}

@Composable
private fun EntryPreview(state: EditorUiState) {
    if (state.title.text.isBlank()) {
        Text("Untitled", style = MaterialTheme.typography.headlineMedium)
    } else {
        RubyText(text = state.title.text, style = MaterialTheme.typography.headlineMedium)
    }
    if (!state.description.isBlank) {
        RubyText(text = state.description, style = MaterialTheme.typography.bodyLarge)
    }
    ExamplesSection(state.examples.toPreviewExamples())
}
