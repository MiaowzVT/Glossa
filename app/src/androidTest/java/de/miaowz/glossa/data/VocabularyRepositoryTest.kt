package de.miaowz.glossa.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.miaowz.glossa.data.db.GlossaDatabase
import de.miaowz.glossa.data.model.ExampleSentence
import de.miaowz.glossa.data.model.GrammarEntryDraft
import de.miaowz.glossa.data.model.VocabularyEntryDraft
import de.miaowz.glossa.richtext.RichText
import de.miaowz.glossa.srs.Rating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class VocabularyRepositoryTest {

    private lateinit var database: GlossaDatabase
    private lateinit var repository: VocabularyRepository
    private lateinit var grammar: GrammarRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GlossaDatabase::class.java,
        ).build()
        repository = VocabularyRepository(database.vocabularyDao(), database.taxonomyDao(), clock = { now }, zone = { ZoneOffset.UTC })
        grammar = GrammarRepository(database.grammarDao(), database.taxonomyDao(), clock = { now })
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun languagesAreSharedButCategoriesAreScopedPerLibrary() = runBlocking {
        val japanese = repository.getOrCreateLanguage("Japanese")
        assertEquals(japanese, grammar.getOrCreateLanguage("japanese"))

        val vocabularyCategory = repository.getOrCreateCategory(japanese.id, "Verbs")
        val grammarCategory = grammar.getOrCreateCategory(japanese.id, "verbs")
        assertNotEquals(vocabularyCategory.id, grammarCategory.id)
        assertEquals(listOf(vocabularyCategory), repository.categories.first())
        assertEquals(listOf(grammarCategory), grammar.categories.first())
    }

    @Test
    fun savesUpdatesAndCountsEntries() = runBlocking {
        val language = repository.getOrCreateLanguage("Japanese")
        val category = repository.getOrCreateCategory(language.id, "Verbs")

        val id = repository.save(
            VocabularyEntryDraft(
                term = " {勉強|べんきょう}する ",
                meaning = " to study ",
                notes = RichText("Often used with を"),
                languageId = language.id,
                categoryId = category.id,
                examples = listOf(
                    ExampleSentence(RichText("{毎日|まいにち}{勉強|べんきょう}する"), RichText("I study every day")),
                    ExampleSentence(RichText.Empty, RichText.Empty),
                ),
            ),
        )

        val saved = repository.getEntry(id)!!
        assertEquals("{勉強|べんきょう}する", saved.term)
        assertEquals("to study", saved.meaning)
        assertEquals(category, saved.category)
        assertEquals(1, saved.examples.size)
        assertEquals(1, repository.entryCount.first())

        now = 2_000L
        repository.save(
            VocabularyEntryDraft(
                id = id,
                term = saved.term,
                meaning = "",
                notes = RichText.Empty,
                languageId = language.id,
                categoryId = null,
                examples = emptyList(),
            ),
        )
        val updated = repository.entries.first().single()
        assertEquals("", updated.meaning)
        assertNull(updated.category)
        assertEquals(emptyList<ExampleSentence>(), updated.examples)
        assertEquals(1_000L, updated.createdAt)
        assertEquals(2_000L, updated.updatedAt)

        // Grammar entries are counted separately.
        grammar.save(GrammarEntryDraft(title = "〜は", description = RichText.Empty, languageId = language.id, categoryId = null, examples = emptyList()))
        assertEquals(1, repository.entryCount.first())
        assertEquals(1, grammar.entryCount.first())

        repository.delete(id)
        assertNull(repository.getEntry(id))
        assertEquals(0, repository.entryCount.first())
    }

    @Test
    fun reviewsRescheduleCardsAndUpdateDueCount() = runBlocking {
        val day = 24 * 60 * 60 * 1000L
        now = 10 * day + 12 * 60 * 60 * 1000L // noon UTC
        val language = repository.getOrCreateLanguage("Japanese")
        fun draft(term: String) = VocabularyEntryDraft(
            term = term, meaning = "", notes = RichText.Empty,
            languageId = language.id, categoryId = null, examples = emptyList(),
        )
        val first = repository.save(draft("{雨|あめ}"))
        now += 1
        val second = repository.save(draft("{雪|ゆき}"))

        // New cards are due immediately, in creation order.
        assertEquals(2, repository.dueCount.first())
        assertEquals(listOf(first, second), repository.getDueEntries().map { it.id })

        repository.review(first, Rating.Good)
        repository.review(second, Rating.Again)
        repository.review(second, Rating.Again)
        assertEquals(0, repository.dueCount.first())
        assertEquals(emptyList<Long>(), repository.getDueEntries().map { it.id })

        // Again → due the next day; Good on a new card → 3 days.
        now = 11 * day
        assertEquals(listOf(second), repository.getDueEntries().map { it.id })
        now = 13 * day
        assertEquals(2, repository.dueCount.first())
        // Already-reviewed cards come before new ones, oldest due first.
        assertEquals(listOf(second, first), repository.getDueEntries().map { it.id })

        // Deleting an entry also deletes its review state.
        repository.delete(second)
        assertEquals(listOf(first), repository.getDueEntries().map { it.id })
    }
}
