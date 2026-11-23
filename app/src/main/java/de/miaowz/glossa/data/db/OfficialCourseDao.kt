package de.miaowz.glossa.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficialCourseDao {

    @Query("SELECT * FROM official_course ORDER BY updatedAt DESC")
    fun observeCourses(): Flow<List<OfficialCourseEntity>>

    @Query("SELECT * FROM official_course WHERE id = :id")
    suspend fun findCourseById(id: Long): OfficialCourseEntity?

    @Upsert
    suspend fun upsertCourses(courses: List<OfficialCourseEntity>)

    @Query("DELETE FROM official_course")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(courses: List<OfficialCourseEntity>) {
        deleteAll()
        upsertCourses(courses)
    }
}