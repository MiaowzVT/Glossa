package de.miaowz.glossa.data

import de.miaowz.glossa.data.db.CategoryType
import de.miaowz.glossa.data.db.ExampleSentenceEntity
import de.miaowz.glossa.data.db.GrammarDao
import de.miaowz.glossa.data.db.GrammarEntryEntity
import de.miaowz.glossa.data.db.GrammarEntryWithDetails
import de.miaowz.glossa.data.db.TaxonomyDao
import de.miaowz.glossa.data.model.ExampleSentence
import de.miaowz.glossa.data.model.GrammarEntry
import de.miaowz.glossa.data.model.GrammarEntryDraft
import de.miaowz.glossa.richtext.RichText
import de.miaowz.glossa.ruby.RubyMarkup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class GrammarRepository(
    private val dao: GrammarDao,
    taxonomyDao: TaxonomyDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : Taxonomy by RoomTaxonomy(taxonomyDao, CategoryType.GRAMMAR) {

    val entries: Flow<List<GrammarEntry>> =
        dao.observeEntries().map { list -> list.map { it.toModel() } }

    val entryCount: Flow<Int> = dao.observeCount()

    fun observeEntry(id: Long): Flow<GrammarEntry?> = dao.observeEntry(id).map { it?.toModel() }

    suspend fun getEntry(id: Long): GrammarEntry? = dao.getEntry(id)?.toModel()

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
        return dao.saveEntry(entry, examples).also { id ->
            Timber.i("Saved grammar entry %d (new=%b, %d examples)", id, draft.id == 0L, examples.size)
        }
    }

    suspend fun delete(id: Long) {
        dao.deleteEntry(id)
        Timber.i("Deleted grammar entry %d", id)
    }
}

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
