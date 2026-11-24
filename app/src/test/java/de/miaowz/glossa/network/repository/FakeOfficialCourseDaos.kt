package de.miaowz.glossa.network.repository

import de.miaowz.glossa.data.db.OfficialCourseDao
import de.miaowz.glossa.data.db.OfficialCourseEntity
import de.miaowz.glossa.data.db.OfficialCourseLectureDao
import de.miaowz.glossa.data.db.OfficialCourseLectureEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for Room, keyed by id; flows emit on every change like Room's. */
class FakeOfficialCourseDao(initial: List<OfficialCourseEntity> = emptyList()) : OfficialCourseDao {
    val rows = MutableStateFlow(initial.associateBy { it.id })

    override fun observeCourses(): Flow<List<OfficialCourseEntity>> =
        rows.map { it.values.sortedByDescending { course -> course.updatedAt } }

    override fun observeCourse(id: Long): Flow<OfficialCourseEntity?> = rows.map { it[id] }

    override suspend fun findCourseById(id: Long): OfficialCourseEntity? = rows.value[id]

    override suspend fun upsertCourses(courses: List<OfficialCourseEntity>) {
        rows.value = rows.value + courses.associateBy { it.id }
    }

    override suspend fun deleteCourse(id: Long) {
        rows.value = rows.value - id
    }

    override suspend fun deleteAll() {
        rows.value = emptyMap()
    }

    override suspend fun replaceAll(courses: List<OfficialCourseEntity>) {
        rows.value = courses.associateBy { it.id }
    }
}

class FakeOfficialCourseLectureDao(initial: List<OfficialCourseLectureEntity> = emptyList()) : OfficialCourseLectureDao {
    val rows = MutableStateFlow(initial.associateBy { it.id })

    override fun observeLecturesForCourse(courseId: Long): Flow<List<OfficialCourseLectureEntity>> =
        rows.map { all -> all.values.filter { it.courseId == courseId }.sortedBy { it.id } }

    override fun observeLecture(id: Long): Flow<OfficialCourseLectureEntity?> = rows.map { it[id] }

    override suspend fun findLectureById(id: Long): OfficialCourseLectureEntity? = rows.value[id]

    override suspend fun findLecturesByCourseId(courseId: Long): List<OfficialCourseLectureEntity> =
        rows.value.values.filter { it.courseId == courseId }

    override suspend fun upsertLectures(lectures: List<OfficialCourseLectureEntity>) {
        rows.value = rows.value + lectures.associateBy { it.id }
    }

    override suspend fun deleteLecturesForCourse(courseId: Long) {
        rows.value = rows.value.filterValues { it.courseId != courseId }
    }

    override suspend fun replaceLecturesForCourse(courseId: Long, lectures: List<OfficialCourseLectureEntity>) {
        rows.value = rows.value.filterValues { it.courseId != courseId } + lectures.associateBy { it.id }
    }
}

fun courseEntity(id: Long, name: String = "Course $id", language: String = "Japanese", updatedAt: Long = id) =
    OfficialCourseEntity(id, name, "Description $id", "N5", language, createdAt = 0, updatedAt = updatedAt)

fun lectureEntity(id: Long, courseId: Long, contents: String = """{"text":"Lecture $id"}""") =
    OfficialCourseLectureEntity(id, courseId, "Lecture $id", contents, createdAt = 0, updatedAt = 0)
