package de.miaowz.glossa.data.db

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
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

    /** Due date of every vocabulary entry; 0 for new entries, which are always due. */
    @Query(
        "SELECT COALESCE(r.due, 0) FROM vocabulary_entries e " +
            "LEFT JOIN vocabulary_review_states r ON r.entryId = e.id",
    )
    fun observeDueDates(): Flow<List<Long>>

    /** Due cards first (oldest first), then new cards in creation order. */
    @Transaction
    @Query(
        "SELECT e.* FROM vocabulary_entries e " +
            "LEFT JOIN vocabulary_review_states r ON r.entryId = e.id " +
            "WHERE r.due IS NULL OR r.due <= :now " +
            "ORDER BY r.due IS NULL, r.due, e.createdAt",
    )
    suspend fun getDueEntries(now: Long): List<VocabularyEntryWithDetails>

    @Query("SELECT * FROM vocabulary_review_states WHERE entryId = :entryId")
    suspend fun getReviewState(entryId: Long): VocabularyReviewStateEntity?

    @Upsert
    suspend fun upsertReviewState(state: VocabularyReviewStateEntity)

    @Insert
    suspend fun insertReviewLog(log: VocabularyReviewLogEntity)

    @Transaction
    suspend fun saveReview(state: VocabularyReviewStateEntity, log: VocabularyReviewLogEntity) {
        upsertReviewState(state)
        insertReviewLog(log)
    }
}
