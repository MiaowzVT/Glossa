package de.miaowz.glossa.data

import de.miaowz.glossa.data.db.CategoryType
import de.miaowz.glossa.data.db.TaxonomyDao
import de.miaowz.glossa.data.db.VocabularyDao
import de.miaowz.glossa.data.db.VocabularyEntryEntity
import de.miaowz.glossa.data.db.VocabularyEntryWithDetails
import de.miaowz.glossa.data.db.VocabularyExampleEntity
import de.miaowz.glossa.data.model.ExampleSentence
import de.miaowz.glossa.data.model.VocabularyEntry
import de.miaowz.glossa.data.model.VocabularyEntryDraft
import de.miaowz.glossa.richtext.RichText
import de.miaowz.glossa.ruby.RubyMarkup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VocabularyRepository(
    private val dao: VocabularyDao,
    taxonomyDao: TaxonomyDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : Taxonomy by RoomTaxonomy(taxonomyDao, CategoryType.VOCABULARY) {

    val entries: Flow<List<VocabularyEntry>> =
        dao.observeEntries().map { list -> list.map { it.toModel() } }

    val entryCount: Flow<Int> = dao.observeCount()

    fun observeEntry(id: Long): Flow<VocabularyEntry?> = dao.observeEntry(id).map { it?.toModel() }

    suspend fun getEntry(id: Long): VocabularyEntry? = dao.getEntry(id)?.toModel()

    /** Saves the draft and returns the id of the entry. */
    suspend fun save(draft: VocabularyEntryDraft): Long {
        val now = clock()
        val createdAt = if (draft.id == 0L) now else dao.getEntry(draft.id)?.entry?.createdAt ?: now
        val entry = VocabularyEntryEntity(
            id = draft.id,
            term = draft.term.trim(),
            meaning = draft.meaning.trim(),
            notes = draft.notes.encode(),
            notesPlain = RubyMarkup.stripToBase(draft.notes.text),
            languageId = draft.languageId,
            categoryId = draft.categoryId,
            createdAt = createdAt,
            updatedAt = now,
        )
        val examples = draft.examples
            .filterNot { it.sentence.isBlank && it.translation.isBlank }
            .map {
                VocabularyExampleEntity(
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

private fun VocabularyEntryWithDetails.toModel() = VocabularyEntry(
    id = entry.id,
    term = entry.term,
    meaning = entry.meaning,
    notes = RichText.decode(entry.notes),
    language = language.toModel(),
    category = category?.toModel(),
    examples = examples.sortedBy { it.position }.map {
        ExampleSentence(RichText.decode(it.sentence), RichText.decode(it.translation))
    },
    createdAt = entry.createdAt,
    updatedAt = entry.updatedAt,
)
