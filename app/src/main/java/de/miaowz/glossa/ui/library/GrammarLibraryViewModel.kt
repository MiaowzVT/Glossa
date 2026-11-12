package de.miaowz.glossa.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.miaowz.glossa.data.GrammarRepository
import de.miaowz.glossa.data.model.Category
import de.miaowz.glossa.data.model.GrammarEntry
import de.miaowz.glossa.data.model.Language
import de.miaowz.glossa.ruby.RubyMarkup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class LibraryFilter(
    val languageId: Long? = null,
    val categoryId: Long? = null,
    val query: String = "",
)

data class LibrarySection(
    val language: Language,
    val entries: List<GrammarEntry>,
)

data class LibraryUiState(
    val isLoading: Boolean = true,
    val filter: LibraryFilter = LibraryFilter(),
    /** Languages that have at least one entry. */
    val languages: List<Language> = emptyList(),
    /** Categories of the selected language that have at least one entry. */
    val categories: List<Category> = emptyList(),
    val sections: List<LibrarySection> = emptyList(),
    val totalEntries: Int = 0,
)

class GrammarLibraryViewModel(repository: GrammarRepository) : ViewModel() {

    private val filter = MutableStateFlow(LibraryFilter())

    val uiState: StateFlow<LibraryUiState> = combine(repository.entries, filter) { entries, filter ->
        val languages = entries.map { it.language }.distinct().sortedBy { it.name.lowercase() }
        // Drop a language filter whose last entry was deleted.
        val effective = if (languages.none { it.id == filter.languageId }) LibraryFilter(query = filter.query) else filter
        val categories = entries
            .filter { it.language.id == effective.languageId }
            .mapNotNull { it.category }
            .distinct()
            .sortedBy { it.name.lowercase() }
        val visible = entries.filter { it.matches(effective) }
        LibraryUiState(
            isLoading = false,
            filter = effective,
            languages = languages,
            categories = categories,
            sections = visible
                .groupBy { it.language }
                .toSortedMap(compareBy { it.name.lowercase() })
                .map { (language, items) ->
                    LibrarySection(
                        language,
                        items.sortedWith(
                            compareBy<GrammarEntry, String?>(nullsLast()) { it.category?.name?.lowercase() }
                                .thenBy { RubyMarkup.stripToBase(it.title).lowercase() },
                        ),
                    )
                },
            totalEntries = entries.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectLanguage(languageId: Long?) =
        filter.update { it.copy(languageId = languageId, categoryId = null) }

    fun selectCategory(categoryId: Long?) = filter.update { it.copy(categoryId = categoryId) }

    fun setQuery(query: String) = filter.update { it.copy(query = query) }
}

private fun GrammarEntry.matches(filter: LibraryFilter): Boolean {
    if (filter.languageId != null && language.id != filter.languageId) return false
    if (filter.categoryId != null && category?.id != filter.categoryId) return false
    val query = filter.query.trim()
    if (query.isEmpty()) return true
    // Search both base text and readings, so "かんじ" finds "{漢字|かんじ}".
    return listOf(title, description.text).any { it.contains(query, ignoreCase = true) } ||
        RubyMarkup.stripToBase(title).contains(query, ignoreCase = true) ||
        RubyMarkup.stripToBase(description.text).contains(query, ignoreCase = true)
}
