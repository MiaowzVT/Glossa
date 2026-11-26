package de.miaowz.glossa.data

import de.miaowz.glossa.data.db.CategoryType
import de.miaowz.glossa.data.db.TaxonomyDao
import de.miaowz.glossa.data.db.VocabularyDao
import de.miaowz.glossa.data.db.VocabularyEntryEntity
import de.miaowz.glossa.data.db.VocabularyEntryWithDetails
import de.miaowz.glossa.data.db.VocabularyExampleEntity
import de.miaowz.glossa.data.db.VocabularyReviewLogEntity
import de.miaowz.glossa.data.db.VocabularyReviewStateEntity
import de.miaowz.glossa.data.model.ExampleSentence
import de.miaowz.glossa.data.model.VocabularyEntry
import de.miaowz.glossa.data.model.VocabularyEntryDraft
import de.miaowz.glossa.richtext.RichText
import de.miaowz.glossa.ruby.RubyMarkup
import de.miaowz.glossa.srs.CardState
import de.miaowz.glossa.srs.Fsrs
import de.miaowz.glossa.srs.Rating
import de.miaowz.glossa.srs.schedule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId

class VocabularyRepository(
    private val dao: VocabularyDao,
    taxonomyDao: TaxonomyDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val fsrs: Fsrs = Fsrs(),
) : Taxonomy by RoomTaxonomy(taxonomyDao, CategoryType.VOCABULARY) {

    val entries: Flow<List<VocabularyEntry>> =
        dao.observeEntries().map { list -> list.map { it.toModel() } }

    val entryCount: Flow<Int> = dao.observeCount()

    /**
     * Number of cards due for repetition, including new cards. Re-evaluated every minute, since
     * cards become due by time passing, not only by database changes.
     */
    val dueCount: Flow<Int> = combine(dao.observeDueDates(), ticker()) { dueDates, now ->
        dueDates.count { it <= now }
    }

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
        return dao.saveEntry(entry, examples).also { id ->
            Timber.i("Saved vocabulary entry %d (new=%b, %d examples)", id, draft.id == 0L, examples.size)
        }
    }

    suspend fun delete(id: Long) {
        dao.deleteEntry(id)
        Timber.i("Deleted vocabulary entry %d", id)
    }

    /** Cards due now, see [VocabularyDao.getDueEntries] for the order. */
    suspend fun getDueEntries(): List<VocabularyEntry> = dao.getDueEntries(clock()).map { it.toModel() }

    /** Records an answer and reschedules the card with FSRS. */
    suspend fun review(entryId: Long, rating: Rating) {
        val now = clock()
        val previous = dao.getReviewState(entryId)?.toCardState()
        val next = fsrs.schedule(previous, rating, now, zone())
        dao.saveReview(next.toEntity(entryId), VocabularyReviewLogEntity(entryId = entryId, rating = rating, reviewedAt = now))
        Timber.d(
            "Reviewed card %d as %s: stability %.2f -> %.2f, difficulty %.2f -> %.2f, reps %d, lapses %d, due %s",
            entryId, rating, previous?.stability ?: 0.0, next.stability, previous?.difficulty ?: 0.0, next.difficulty,
            next.reps, next.lapses, Instant.ofEpochMilli(next.due),
        )
    }

    private fun ticker(): Flow<Long> = flow {
        while (true) {
            emit(clock())
            delay(60_000)
        }
    }
}

private fun VocabularyReviewStateEntity.toCardState() =
    CardState(stability, difficulty, due, lastReview, reps, lapses)

private fun CardState.toEntity(entryId: Long) =
    VocabularyReviewStateEntity(entryId, stability, difficulty, due, lastReview, reps, lapses)

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
