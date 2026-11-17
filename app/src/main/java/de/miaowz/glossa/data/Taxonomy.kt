package de.miaowz.glossa.data

import de.miaowz.glossa.data.db.CategoryEntity
import de.miaowz.glossa.data.db.CategoryType
import de.miaowz.glossa.data.db.LanguageEntity
import de.miaowz.glossa.data.db.TaxonomyDao
import de.miaowz.glossa.data.model.Category
import de.miaowz.glossa.data.model.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Languages (shared by all libraries) and the categories of one library. */
interface Taxonomy {
    val languages: Flow<List<Language>>
    val categories: Flow<List<Category>>

    /** Returns the language called [name] (case-insensitive), creating it if needed. */
    suspend fun getOrCreateLanguage(name: String): Language

    suspend fun getOrCreateCategory(languageId: Long, name: String): Category
}

internal class RoomTaxonomy(private val dao: TaxonomyDao, private val type: CategoryType) : Taxonomy {

    override val languages: Flow<List<Language>> =
        dao.observeLanguages().map { list -> list.map { it.toModel() } }

    override val categories: Flow<List<Category>> =
        dao.observeCategories(type).map { list -> list.map { it.toModel() } }

    override suspend fun getOrCreateLanguage(name: String): Language {
        val trimmed = name.trim()
        val existing = dao.findLanguage(trimmed)
        if (existing != null) return existing.toModel()
        val id = dao.insertLanguage(LanguageEntity(name = trimmed))
        return Language(id, trimmed)
    }

    override suspend fun getOrCreateCategory(languageId: Long, name: String): Category {
        val trimmed = name.trim()
        val existing = dao.findCategory(languageId, type, trimmed)
        if (existing != null) return existing.toModel()
        val id = dao.insertCategory(CategoryEntity(languageId = languageId, type = type, name = trimmed))
        return Category(id, languageId, trimmed)
    }
}

internal fun LanguageEntity.toModel() = Language(id, name)

internal fun CategoryEntity.toModel() = Category(id, languageId, name)
