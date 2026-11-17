package de.miaowz.glossa.ui.vocabulary

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import de.miaowz.glossa.data.VocabularyRepository
import de.miaowz.glossa.data.model.ExampleSentence
import de.miaowz.glossa.data.model.VocabularyEntryDraft
import de.miaowz.glossa.richtext.RichText
import de.miaowz.glossa.ui.common.CategoryOptions
import de.miaowz.glossa.ui.common.ExampleDraft
import de.miaowz.glossa.ui.common.SavedEntry
import de.miaowz.glossa.ui.navigation.VocabularyEditorRoute
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

data class VocabularyEditorUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val term: TextFieldValue = TextFieldValue(),
    val meaning: TextFieldValue = TextFieldValue(),
    val notes: RichText = RichText.Empty,
    val languageId: Long? = null,
    val categoryId: Long? = null,
    val examples: List<ExampleDraft> = emptyList(),
    val isDirty: Boolean = false,
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    val saved: SavedEntry? = null,
) {
    val termError: String? get() = if (showErrors && term.text.isBlank()) "Enter a word or phrase" else null
    val languageError: String? get() = if (showErrors && languageId == null) "Choose a language" else null
}

class VocabularyEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: VocabularyRepository,
) : ViewModel() {

    private val entryId = savedStateHandle.toRoute<VocabularyEditorRoute>().entryId
    private var nextExampleKey = 0L

    private val _uiState = MutableStateFlow(VocabularyEditorUiState(isNew = entryId == 0L))
    val uiState: StateFlow<VocabularyEditorUiState> = _uiState.asStateFlow()

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
                    term = TextFieldValue(entry.term, TextRange(entry.term.length)),
                    meaning = TextFieldValue(entry.meaning, TextRange(entry.meaning.length)),
                    notes = entry.notes,
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

    private fun edit(transform: (VocabularyEditorUiState) -> VocabularyEditorUiState) {
        _uiState.update { transform(it).copy(isDirty = true) }
    }

    fun setTerm(value: TextFieldValue) {
        // Selection-only changes don't count as edits.
        if (value.text == _uiState.value.term.text) _uiState.update { it.copy(term = value) }
        else edit { it.copy(term = value) }
    }

    fun setMeaning(value: TextFieldValue) {
        if (value.text == _uiState.value.meaning.text) _uiState.update { it.copy(meaning = value) }
        else edit { it.copy(meaning = value) }
    }

    fun setNotes(value: RichText) {
        if (value != _uiState.value.notes) edit { it.copy(notes = value) }
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
        if (state.term.text.isBlank() || languageId == null) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val id = repository.save(
                VocabularyEntryDraft(
                    id = if (state.isNew) 0L else entryId,
                    term = state.term.text,
                    meaning = state.meaning.text,
                    notes = state.notes,
                    languageId = languageId,
                    categoryId = state.categoryId,
                    examples = state.examples.map { ExampleSentence(it.sentence, it.translation) },
                ),
            )
            _uiState.update { it.copy(isSaving = false, isDirty = false, saved = SavedEntry(id, state.isNew)) }
        }
    }
}
