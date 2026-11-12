package de.miaowz.glossa.data

import de.miaowz.glossa.data.db.CategoryEntity
import de.miaowz.glossa.data.db.ExampleSentenceEntity
import de.miaowz.glossa.data.db.GrammarDao
import de.miaowz.glossa.data.db.GrammarEntryEntity
import de.miaowz.glossa.data.db.GrammarEntryWithDetails
import de.miaowz.glossa.data.db.LanguageEntity
import de.miaowz.glossa.data.model.Category
import de.miaowz.glossa.data.model.ExampleSentence
import de.miaowz.glossa.data.model.GrammarEntry
import de.miaowz.glossa.data.model.GrammarEntryDraft
import de.miaowz.glossa.data.model.Language
import de.miaowz.glossa.richtext.RichText
import de.miaowz.glossa.ruby.RubyMarkup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GrammarRepository(
    private val dao: GrammarDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val languages: Flow<List<Language>> =
        dao.observeLanguages().map { list -> list.map { it.toModel() } }

    val categories: Flow<List<Category>> =
        dao.observeCategories().map { list -> list.map { it.toModel() } }

    val entries: Flow<List<GrammarEntry>> =
        dao.observeEntries().map { list -> list.map { it.toModel() } }

    fun observeEntry(id: Long): Flow<GrammarEntry?> = dao.observeEntry(id).map { it?.toModel() }

    suspend fun getEntry(id: Long): GrammarEntry? = dao.getEntry(id)?.toModel()

    /** Returns the language called [name] (case-insensitive), creating it if needed. */
    suspend fun getOrCreateLanguage(name: String): Language {
        val trimmed = name.trim()
        val existing = dao.findLanguage(trimmed)
        if (existing != null) return existing.toModel()
        val id = dao.insertLanguage(LanguageEntity(name = trimmed))
        return Language(id, trimmed)
    }

    suspend fun getOrCreateCategory(languageId: Long, name: String): Category {
        val trimmed = name.trim()
        val existing = dao.findCategory(languageId, trimmed)
        if (existing != null) return existing.toModel()
        val id = dao.insertCategory(CategoryEntity(languageId = languageId, name = trimmed))
        return Category(id, languageId, trimmed)
    }

    /** Saves the draft and returns the id of the entry. */
    suspend fun save(draft: GrammarEntryDraft): Long {
        val now = clock()
        val createdAt = if (draft.id == 0L) now else dao.getEntry(draft.id)?.entry?.createdAt ?: now
        val entry = GrammarEntryEntity(
            id = draft.id,
            title = draft.title.trim(),
            description = draft.description.encode(),
            descriptionPlain = RubyMarkup.stripToBase(draft.description.text),
            languageId = draft.languageId,
            categoryId = draft.categoryId,
            createdAt = createdAt,
            updatedAt = now,
        )
        val examples = draft.examples
            .filterNot { it.sentence.isBlank && it.translation.isBlank }
            .map {
                ExampleSentenceEntity(
                    entryId = draft.id,
                    position = 0,
                    sentence = it.sentence.encode(),
                    translation = it.translation.encode(),
                )
            }
        return dao.saveEntry(entry, examples)
    }

    suspend fun delete(id: Long) = dao.deleteEntry(id)
}

private fun LanguageEntity.toModel() = Language(id, name)

private fun CategoryEntity.toModel() = Category(id, languageId, name)

private fun GrammarEntryWithDetails.toModel() = GrammarEntry(
    id = entry.id,
    title = entry.title,
    description = RichText.decode(entry.description),
    language = language.toModel(),
    category = category?.toModel(),
    examples = examples.sortedBy { it.position }.map {
        ExampleSentence(RichText.decode(it.sentence), RichText.decode(it.translation))
    },
    createdAt = entry.createdAt,
    updatedAt = entry.updatedAt,
)
