package de.miaowz.glossa.network.repository

import de.miaowz.glossa.data.db.OfficialCourseLectureDao
import de.miaowz.glossa.data.db.OfficialCourseLectureEntity
import de.miaowz.glossa.data.dto.toEntity
import de.miaowz.glossa.data.model.OfficialLecture
import de.miaowz.glossa.network.OfficialCourseLectureApi
import de.miaowz.glossa.richtext.RichText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Lectures of official courses, cached per course in Room (see [OfficialCourseRepository]). */
class OfficialCourseLectureRepository(
    private val api: OfficialCourseLectureApi,
    private val dao: OfficialCourseLectureDao,
) {
    fun observeLectures(courseId: Long): Flow<List<OfficialLecture>> =
        dao.observeLecturesForCourse(courseId).map { lectures -> lectures.map { it.toModel() } }

    fun observeLecture(id: Long): Flow<OfficialLecture?> = dao.observeLecture(id).map { it?.toModel() }

    /** Replaces the cached lectures of [courseId] with the backend's list. */
    suspend fun refreshLectures(courseId: Long) {
        val remote = api.getLecturesForCourse(courseId)
        // The endpoint is per course, but don't trust rows of other courses to end up here.
        dao.replaceLecturesForCourse(courseId, remote.filter { it.courseId == courseId }.map { it.toEntity() })
    }
}

private fun OfficialCourseLectureEntity.toModel() = OfficialLecture(
    id = id,
    courseId = courseId,
    name = lectureName,
    contents = RichText.decode(lectureContents),
    updatedAt = updatedAt,
)
