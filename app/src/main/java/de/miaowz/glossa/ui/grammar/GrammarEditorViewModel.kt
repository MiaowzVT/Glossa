package de.miaowz.glossa.ui.grammar

import androidx.annotation.StringRes
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import de.miaowz.glossa.R
import de.miaowz.glossa.data.GrammarRepository
import de.miaowz.glossa.data.model.ExampleSentence
import de.miaowz.glossa.data.model.GrammarEntryDraft
import de.miaowz.glossa.richtext.RichText
import de.miaowz.glossa.ui.common.CategoryOptions
import de.miaowz.glossa.ui.common.ExampleDraft
import de.miaowz.glossa.ui.common.SavedEntry
import de.miaowz.glossa.ui.navigation.GrammarEditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val title: TextFieldValue = TextFieldValue(),
    val description: RichText = RichText.Empty,
    val languageId: Long? = null,
    val categoryId: Long? = null,
    val examples: List<ExampleDraft> = emptyList(),
    val isDirty: Boolean = false,
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    val saved: SavedEntry? = null,
) {
    @get:StringRes
    val titleError: Int? get() = if (showErrors && title.text.isBlank()) R.string.error_title_required else null
    @get:StringRes
    val languageError: Int? get() = if (showErrors && languageId == null) R.string.error_language_required else null
}

class GrammarEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: GrammarRepository,
) : ViewModel() {

    private val entryId = savedStateHandle.toRoute<GrammarEditorRoute>().entryId
    private var nextExampleKey = 0L

    private val _uiState = MutableStateFlow(EditorUiState(isNew = entryId == 0L))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    val categoryOptions: StateFlow<CategoryOptions> = combine(
        repository.languages,
        repository.categories,
        _uiState.map { it.languageId }.distinctUntilChanged(),
    ) { languages, categories, languageId ->
        CategoryOptions(languages, categories.filter { it.languageId == languageId })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryOptions())

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (entryId == 0L) {
            // Continue in the language that was used last.
            val lastLanguage = repository.entries.first().firstOrNull()?.language?.id
            _uiState.update {
                it.copy(isLoading = false, languageId = lastLanguage, examples = listOf(newExample()))
            }
            return
        }
        val entry = repository.getEntry(entryId)
        _uiState.update { state ->
            if (entry == null) {
                state.copy(isLoading = false, isNew = true, examples = listOf(newExample()))
            } else {
                state.copy(
                    isLoading = false,
                    title = TextFieldValue(entry.title, TextRange(entry.title.length)),
                    description = entry.description,
                    languageId = entry.language.id,
                    categoryId = entry.category?.id,
                    examples = entry.examples
                        .map { ExampleDraft(nextExampleKey++, it.sentence, it.translation) }
                        .ifEmpty { listOf(newExample()) },
                )
            }
        }
    }

    private fun newExample() = ExampleDraft(nextExampleKey++)

    private fun edit(transform: (EditorUiState) -> EditorUiState) {
        _uiState.update { transform(it).copy(isDirty = true) }
    }

    fun setTitle(value: TextFieldValue) {
        // Selection-only changes don't count as edits.
        if (value.text == _uiState.value.title.text) _uiState.update { it.copy(title = value) }
        else edit { it.copy(title = value) }
    }

    fun setDescription(value: RichText) {
        if (value != _uiState.value.description) edit { it.copy(description = value) }
    }

    fun selectLanguage(languageId: Long) {
        if (languageId != _uiState.value.languageId) edit { it.copy(languageId = languageId, categoryId = null) }
    }

    fun selectCategory(categoryId: Long?) {
        if (categoryId != _uiState.value.categoryId) edit { it.copy(categoryId = categoryId) }
    }

    fun createLanguage(name: String) {
        viewModelScope.launch { selectLanguage(repository.getOrCreateLanguage(name).id) }
    }

    fun createCategory(name: String) {
        val languageId = _uiState.value.languageId ?: return
        viewModelScope.launch { selectCategory(repository.getOrCreateCategory(languageId, name).id) }
    }

    fun addExample() = edit { it.copy(examples = it.examples + newExample()) }

    fun removeExample(key: Long) = edit { state ->
        state.copy(examples = state.examples.filterNot { it.key == key })
    }

    fun updateExample(key: Long, transform: (ExampleDraft) -> ExampleDraft) {
        val current = _uiState.value.examples.firstOrNull { it.key == key } ?: return
        val updated = transform(current)
        if (updated == current) return
        edit { state -> state.copy(examples = state.examples.map { if (it.key == key) updated else it }) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        val languageId = state.languageId
        if (state.title.text.isBlank() || languageId == null) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val id = repository.save(
                GrammarEntryDraft(
                    id = if (state.isNew) 0L else entryId,
                    title = state.title.text,
                    description = state.description,
                    languageId = languageId,
                    categoryId = state.categoryId,
                    examples = state.examples.map { ExampleSentence(it.sentence, it.translation) },
                ),
            )
            _uiState.update { it.copy(isSaving = false, isDirty = false, saved = SavedEntry(id, state.isNew)) }
        }
    }
}
