package de.miaowz.glossa.network.repository

import de.miaowz.glossa.data.db.OfficialCourseDao
import de.miaowz.glossa.data.db.OfficialCourseEntity
import de.miaowz.glossa.data.dto.toEntity
import de.miaowz.glossa.network.OfficialCourseApi
import kotlinx.coroutines.flow.Flow

class OfficialCourseRepository(
    private val api: OfficialCourseApi,
    private val dao: OfficialCourseDao
) {
    val courses: Flow<List<OfficialCourseEntity>> = dao.observeCourses()

    suspend fun refresh() {
        val remote = api.getCourses()
        dao.replaceAll(remote.map { it.toEntity() })
    }
}