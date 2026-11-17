package de.miaowz.glossa.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.miaowz.glossa.data.db.GlossaDatabase
import de.miaowz.glossa.data.model.ExampleSentence
import de.miaowz.glossa.data.model.GrammarEntryDraft
import de.miaowz.glossa.richtext.RichSpan
import de.miaowz.glossa.richtext.RichText
import de.miaowz.glossa.richtext.SpanFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrammarRepositoryTest {

    private lateinit var database: GlossaDatabase
    private lateinit var repository: GrammarRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GlossaDatabase::class.java,
        ).build()
        repository = GrammarRepository(database.grammarDao(), database.taxonomyDao(), clock = { now })
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun languagesAndCategoriesAreReusedCaseInsensitively() = runBlocking {
        val japanese = repository.getOrCreateLanguage("Japanese")
        assertEquals(japanese, repository.getOrCreateLanguage(" japanese "))

        val particles = repository.getOrCreateCategory(japanese.id, "Particles")
        assertEquals(particles, repository.getOrCreateCategory(japanese.id, "particles"))

        val korean = repository.getOrCreateLanguage("Korean")
        assertEquals(korean.id, repository.getOrCreateCategory(korean.id, "Particles").languageId)
    }

    @Test
    fun savesAndUpdatesEntryWithExamples() = runBlocking {
        val language = repository.getOrCreateLanguage("Japanese")
        val category = repository.getOrCreateCategory(language.id, "Particles")
        val description = RichText("{助詞|じょし} は", listOf(RichSpan(0, 7, SpanFormat.Bold)))

        val id = repository.save(
            GrammarEntryDraft(
                title = "〜は",
                description = description,
                languageId = language.id,
                categoryId = category.id,
                examples = listOf(
                    ExampleSentence(RichText("{私|わたし}は学生です"), RichText("I am a student")),
                    ExampleSentence(RichText.Empty, RichText.Empty),
                ),
            ),
        )

        val saved = repository.getEntry(id)!!
        assertEquals(description, saved.description)
        assertEquals(category, saved.category)
        assertEquals(1, saved.examples.size)
        assertEquals(1_000L, saved.createdAt)

        now = 2_000L
        repository.save(
            GrammarEntryDraft(
                id = id,
                title = "〜は (topic)",
                description = description,
                languageId = language.id,
                categoryId = null,
                examples = listOf(ExampleSentence(RichText("b")), ExampleSentence(RichText("a"))),
            ),
        )
        val updated = repository.entries.first().single()
        assertEquals("〜は (topic)", updated.title)
        assertNull(updated.category)
        assertEquals(listOf("b", "a"), updated.examples.map { it.sentence.text })
        assertEquals(1_000L, updated.createdAt)
        assertEquals(2_000L, updated.updatedAt)

        repository.delete(id)
        assertNull(repository.getEntry(id))
    }
}
