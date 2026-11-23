package de.miaowz.glossa.network.repository

import de.miaowz.glossa.data.db.OfficialCourseDao
import de.miaowz.glossa.data.db.OfficialCourseEntity
import de.miaowz.glossa.data.dto.toEntity
import de.miaowz.glossa.data.model.OfficialCourse
import de.miaowz.glossa.network.OfficialCourseApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Official courses: Room is the source of truth for the UI (works offline), [refresh] and
 * [refreshCourse] sync it from the backend and throw on network/HTTP/parse errors.
 */
class OfficialCourseRepository(
    private val api: OfficialCourseApi,
    private val dao: OfficialCourseDao,
) {
    val courses: Flow<List<OfficialCourse>> =
        dao.observeCourses().map { courses -> courses.map { it.toModel() } }

    fun observeCourse(id: Long): Flow<OfficialCourse?> = dao.observeCourse(id).map { it?.toModel() }

    /** Replaces all cached courses with the backend's list. */
    suspend fun refresh() {
        val remote = api.getCourses()
        dao.replaceAll(remote.map { it.toEntity() })
    }

    /**
     * Updates one cached course. Returns `false` (and drops the cached copy) when the backend no
     * longer has it.
     */
    suspend fun refreshCourse(id: Long): Boolean {
        val remote = api.getCourse(id)
        if (remote == null) {
            dao.deleteCourse(id)
            return false
        }
        dao.upsertCourses(listOf(remote.toEntity()))
        return true
    }
}

private fun OfficialCourseEntity.toModel() = OfficialCourse(
    id = id,
    name = courseName,
    description = courseDescription,
    level = courseLevel,
    language = courseLanguage,
    updatedAt = updatedAt,
)
