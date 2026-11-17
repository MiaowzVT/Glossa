package de.miaowz.glossa.data.db

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val TAG = "Glossa::GrammarDao"
@Dao
interface GrammarDao {

    @Transaction
    @Query("SELECT * FROM grammar_entries ORDER BY updatedAt DESC")
    fun observeEntries(): Flow<List<GrammarEntryWithDetails>>

    @Query("SELECT COUNT(*) FROM grammar_entries")
    fun observeCount(): Flow<Int>

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

        Log.i(TAG, "Saving Grammar Entry id=${id} | title=${entry.title};description=${entry.description};language_id=${entry.languageId}")

        return id
    }
}
