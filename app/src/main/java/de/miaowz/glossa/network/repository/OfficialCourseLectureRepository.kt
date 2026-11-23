package de.miaowz.glossa.network.repository

import de.miaowz.glossa.data.db.OfficialCourseLectureDao
import de.miaowz.glossa.data.db.OfficialCourseLectureEntity
import de.miaowz.glossa.data.dto.toEntity
import de.miaowz.glossa.network.OfficialCourseLectureApi
import kotlinx.coroutines.flow.Flow

class OfficialCourseLectureRepository(
    private val api: OfficialCourseLectureApi,
    private val dao: OfficialCourseLectureDao
) {
    val lectures: Flow<List<OfficialCourseLectureEntity>> = dao.observeLectures()

    suspend fun refresh() {
        val remote = api.getAllLectures()
        dao.replaceAll(remote.map { it.toEntity() })
    }
}