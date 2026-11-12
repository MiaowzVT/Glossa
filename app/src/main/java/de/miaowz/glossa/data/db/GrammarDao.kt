package de.miaowz.glossa.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GrammarDao {

    @Query("SELECT * FROM languages ORDER BY name")
    fun observeLanguages(): Flow<List<LanguageEntity>>

    @Query("SELECT * FROM categories ORDER BY name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM languages WHERE name = :name LIMIT 1")
    suspend fun findLanguage(name: String): LanguageEntity?

    @Query("SELECT * FROM categories WHERE languageId = :languageId AND name = :name LIMIT 1")
    suspend fun findCategory(languageId: Long, name: String): CategoryEntity?

    @Insert
    suspend fun insertLanguage(language: LanguageEntity): Long

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Transaction
    @Query("SELECT * FROM grammar_entries ORDER BY updatedAt DESC")
    fun observeEntries(): Flow<List<GrammarEntryWithDetails>>

    @Transaction
    @Query("SELECT * FROM grammar_entries WHERE id = :id")
    fun observeEntry(id: Long): Flow<GrammarEntryWithDetails?>

    @Transaction
    @Query("SELECT * FROM grammar_entries WHERE id = :id")
    suspend fun getEntry(id: Long): GrammarEntryWithDetails?

    @Insert
    suspend fun insertEntry(entry: GrammarEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: GrammarEntryEntity)

    @Query("DELETE FROM grammar_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("DELETE FROM example_sentences WHERE entryId = :entryId")
    suspend fun deleteExamples(entryId: Long)

    @Insert
    suspend fun insertExamples(examples: List<ExampleSentenceEntity>)

    /** Inserts or updates [entry] and replaces its examples. Returns the entry id. */
    @Transaction
    suspend fun saveEntry(entry: GrammarEntryEntity, examples: List<ExampleSentenceEntity>): Long {
        val id = if (entry.id == 0L) insertEntry(entry) else entry.id.also { updateEntry(entry) }
        deleteExamples(id)
        insertExamples(examples.mapIndexed { index, example -> example.copy(entryId = id, position = index) })
        return id
    }
}
