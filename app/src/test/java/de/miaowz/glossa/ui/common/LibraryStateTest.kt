package de.miaowz.glossa.ui.common

import de.miaowz.glossa.data.model.Category
import de.miaowz.glossa.data.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryStateTest {

    private data class Item(val id: Long, val title: String, val language: Language, val category: Category?)

    private val keys = LibraryKeys<Item>(
        language = { it.language },
        category = { it.category },
        title = { it.title },
        searchText = { listOf(it.title) },
    )

    private val japanese = Language(1, "Japanese")
    private val korean = Language(2, "korean")
    private val particles = Category(1, japanese.id, "Particles")
    private val verbs = Category(2, japanese.id, "verbs")

    private val items = listOf(
        Item(1, "{食|た}べる", japanese, verbs),
        Item(2, "は", japanese, particles),
        Item(3, "{雨|あめ}", japanese, null),
        Item(4, "이다", korean, null),
    )

    @Test
    fun groupsByLanguageAndSortsByCategoryThenTitle() {
        val state = libraryState(items, LibraryFilter(), keys)
        assertEquals(listOf(japanese, korean), state.sections.map { it.language })
        assertEquals(listOf(2L, 1L, 3L), state.sections[0].entries.map { it.id })
        assertEquals(4, state.totalEntries)
        assertEquals(emptyList<Category>(), state.categories)
    }

    @Test
    fun filtersByLanguageAndCategory() {
        val byLanguage = libraryState(items, LibraryFilter(languageId = japanese.id), keys)
        assertEquals(listOf(particles, verbs), byLanguage.categories)
        assertEquals(listOf(japanese), byLanguage.sections.map { it.language })

        val byCategory = libraryState(items, LibraryFilter(languageId = japanese.id, categoryId = verbs.id), keys)
        assertEquals(listOf(1L), byCategory.sections.single().entries.map { it.id })
    }

    @Test
    fun searchMatchesReadingsAndBaseText() {
        assertEquals(listOf(3L), ids(libraryState(items, LibraryFilter(query = "あめ"), keys)))
        assertEquals(listOf(1L), ids(libraryState(items, LibraryFilter(query = "食べる"), keys)))
        assertEquals(emptyList<Long>(), ids(libraryState(items, LibraryFilter(query = "xyz"), keys)))
    }

    @Test
    fun dropsLanguageFilterWithoutEntries() {
        val state = libraryState(items, LibraryFilter(languageId = 99, categoryId = 5, query = "は"), keys)
        assertNull(state.filter.languageId)
        assertNull(state.filter.categoryId)
        assertEquals("は", state.filter.query)
    }

    private fun ids(state: LibraryUiState<Item>) = state.sections.flatMap { it.entries }.map { it.id }
}
