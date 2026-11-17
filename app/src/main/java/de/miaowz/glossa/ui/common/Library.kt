package de.miaowz.glossa.ui.common

import de.miaowz.glossa.data.model.Category
import de.miaowz.glossa.data.model.Language
import de.miaowz.glossa.ruby.RubyMarkup

data class LibraryFilter(
    val languageId: Long? = null,
    val categoryId: Long? = null,
    val query: String = "",
)

data class LibrarySection<T>(
    val language: Language,
    val entries: List<T>,
)

data class LibraryUiState<T>(
    val isLoading: Boolean = true,
    val filter: LibraryFilter = LibraryFilter(),
    /** Languages that have at least one entry. */
    val languages: List<Language> = emptyList(),
    /** Categories of the selected language that have at least one entry. */
    val categories: List<Category> = emptyList(),
    val sections: List<LibrarySection<T>> = emptyList(),
    val totalEntries: Int = 0,
)

/** How a library reads its entries; [searchText] may contain ruby markup. */
class LibraryKeys<T>(
    val language: (T) -> Language,
    val category: (T) -> Category?,
    val title: (T) -> String,
    val searchText: (T) -> List<String>,
)

/**
 * Filters and groups [entries]: sections per language (sorted case-insensitively), entries by
 * category then stripped title.
 */
fun <T> libraryState(entries: List<T>, filter: LibraryFilter, keys: LibraryKeys<T>): LibraryUiState<T> {
    val languages = entries.map(keys.language).distinct().sortedBy { it.name.lowercase() }
    // Drop a language filter whose last entry was deleted.
    val effective = if (languages.none { it.id == filter.languageId }) LibraryFilter(query = filter.query) else filter
    val categories = entries
        .filter { keys.language(it).id == effective.languageId }
        .mapNotNull(keys.category)
        .distinct()
        .sortedBy { it.name.lowercase() }
    val visible = entries.filter { keys.matches(it, effective) }
    return LibraryUiState(
        isLoading = false,
        filter = effective,
        languages = languages,
        categories = categories,
        sections = visible
            .groupBy(keys.language)
            .toSortedMap(compareBy { it.name.lowercase() })
            .map { (language, items) ->
                LibrarySection(
                    language,
                    items.sortedWith(
                        compareBy<T, String?>(nullsLast()) { keys.category(it)?.name?.lowercase() }
                            .thenBy { RubyMarkup.stripToBase(keys.title(it)).lowercase() },
                    ),
                )
            },
        totalEntries = entries.size,
    )
}

private fun <T> LibraryKeys<T>.matches(entry: T, filter: LibraryFilter): Boolean {
    if (filter.languageId != null && language(entry).id != filter.languageId) return false
    if (filter.categoryId != null && category(entry)?.id != filter.categoryId) return false
    val query = filter.query.trim()
    if (query.isEmpty()) return true
    // Search both base text and readings, so "かんじ" finds "{漢字|かんじ}".
    return searchText(entry).any {
        it.contains(query, ignoreCase = true) || RubyMarkup.stripToBase(it).contains(query, ignoreCase = true)
    }
}
