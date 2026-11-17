package de.miaowz.glossa.ui.grammar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.miaowz.glossa.data.GrammarRepository
import de.miaowz.glossa.data.model.GrammarEntry
import de.miaowz.glossa.ui.common.LibraryFilter
import de.miaowz.glossa.ui.common.LibraryKeys
import de.miaowz.glossa.ui.common.LibraryUiState
import de.miaowz.glossa.ui.common.libraryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

private val grammarKeys = LibraryKeys<GrammarEntry>(
    language = { it.language },
    category = { it.category },
    title = { it.title },
    searchText = { listOf(it.title, it.description.text) },
)

class GrammarLibraryViewModel(repository: GrammarRepository) : ViewModel() {

    private val filter = MutableStateFlow(LibraryFilter())

    val uiState: StateFlow<LibraryUiState<GrammarEntry>> =
        combine(repository.entries, filter) { entries, filter -> libraryState(entries, filter, grammarKeys) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectLanguage(languageId: Long?) =
        filter.update { it.copy(languageId = languageId, categoryId = null) }

    fun selectCategory(categoryId: Long?) = filter.update { it.copy(categoryId = categoryId) }

    fun setQuery(query: String) = filter.update { it.copy(query = query) }
}
