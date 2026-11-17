package de.miaowz.glossa.data.db

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val TAG = "Glossa::VocabularyDao"

@Dao
interface VocabularyDao {

    @Transaction
    @Query("SELECT * FROM vocabulary_entries ORDER BY updatedAt DESC")
    fun observeEntries(): Flow<List<VocabularyEntryWithDetails>>

    @Query("SELECT COUNT(*) FROM vocabulary_entries")
    fun observeCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM vocabulary_entries WHERE id = :id")
    fun observeEntry(id: Long): Flow<VocabularyEntryWithDetails?>

    @Transaction
    @Query("SELECT * FROM vocabulary_entries WHERE id = :id")
    suspend fun getEntry(id: Long): VocabularyEntryWithDetails?

    @Insert
    suspend fun insertEntry(entry: VocabularyEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: VocabularyEntryEntity)

    @Query("DELETE FROM vocabulary_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("DELETE FROM vocabulary_examples WHERE entryId = :entryId")
    suspend fun deleteExamples(entryId: Long)

    @Insert
    suspend fun insertExamples(examples: List<VocabularyExampleEntity>)

    /** Inserts or updates [entry] and replaces its examples. Returns the entry id. */
    @Transaction
    suspend fun saveEntry(entry: VocabularyEntryEntity, examples: List<VocabularyExampleEntity>): Long {
        val id = if (entry.id == 0L) insertEntry(entry) else entry.id.also { updateEntry(entry) }
        deleteExamples(id)
        insertExamples(examples.mapIndexed { index, example -> example.copy(entryId = id, position = index) })

        Log.i(TAG, "Saving Vocabulary Entry id=${id} | term=${entry.term};meaning=${entry.meaning};language_id=${entry.languageId}")

        return id
    }
}
