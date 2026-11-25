package de.miaowz.glossa.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficialCourseLectureDao {

    /** Lectures of one course in creation order (the backend has no explicit position). */
    @Query("SELECT * FROM official_course_lecture WHERE courseId = :courseId ORDER BY id")
    fun observeLecturesForCourse(courseId: Long): Flow<List<OfficialCourseLectureEntity>>

    @Query("SELECT * FROM official_course_lecture WHERE id = :id")
    fun observeLecture(id: Long): Flow<OfficialCourseLectureEntity?>

    @Query("SELECT * FROM official_course_lecture WHERE id = :id")
    suspend fun findLectureById(id: Long): OfficialCourseLectureEntity?

    @Query("SELECT * FROM official_course_lecture WHERE courseId = :courseId")
    suspend fun findLecturesByCourseId(courseId: Long): List<OfficialCourseLectureEntity>

    @Upsert
    suspend fun upsertLectures(lectures: List<OfficialCourseLectureEntity>)

    @Query("DELETE FROM official_course_lecture WHERE courseId = :courseId")
    suspend fun deleteLecturesForCourse(courseId: Long)

    /** Replaces the lectures of one course; other courses' lectures are untouched. */
    @Transaction
    suspend fun replaceLecturesForCourse(courseId: Long, lectures: List<OfficialCourseLectureEntity>) {
        deleteLecturesForCourse(courseId)
        upsertLectures(lectures)
    }
}
