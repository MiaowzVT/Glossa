package de.miaowz.glossa.data.dto

import de.miaowz.glossa.data.db.OfficialCourseLectureEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One row of `GET /api/course/:id/lectures` (backend table `official_course_lecture`).
 * [lectureContents] is `RichText` JSON as written by the admin panel; it is stored as-is and
 * decoded by the repository. The timestamps are nullable columns in the backend.
 */
@Serializable
data class OfficialCourseLectureDto(
    val id: Long,
    @SerialName("course_id") val courseId: Long,
    @SerialName("lecture_name") val lectureName: String = "",
    @SerialName("lecture_contents") val lectureContents: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun OfficialCourseLectureDto.toEntity() = OfficialCourseLectureEntity(
    id = id,
    courseId = courseId,
    lectureName = lectureName,
    lectureContents = lectureContents,
    createdAt = parseBackendTimestamp(createdAt),
    updatedAt = parseBackendTimestamp(updatedAt),
)
