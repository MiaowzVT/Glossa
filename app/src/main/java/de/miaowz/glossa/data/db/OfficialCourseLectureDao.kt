package de.miaowz.glossa.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficialCourseLectureDao {

    @Query("SELECT * FROM official_course_lecture ORDER BY updatedAt DESC")
    fun observeLectures(): Flow<List<OfficialCourseLectureEntity>>

    @Query("SELECT * FROM official_course_lecture WHERE id = :id")
    suspend fun findLectureById(id: Long): OfficialCourseLectureEntity?

    @Query("SELECT * FROM official_course_lecture WHERE courseId = :courseId")
    suspend fun findLecturesByCourseId(courseId: Long): List<OfficialCourseLectureEntity>
    @Upsert
    suspend fun upsertLectures(courses: List<OfficialCourseLectureEntity>)

    @Query("DELETE FROM official_course_lecture")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(lectures: List<OfficialCourseLectureEntity>) {
        deleteAll()
        upsertLectures(lectures)
    }
}