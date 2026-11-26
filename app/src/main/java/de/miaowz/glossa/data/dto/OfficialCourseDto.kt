package de.miaowz.glossa.data.dto

import de.miaowz.glossa.data.db.OfficialCourseEntity
import de.miaowz.glossa.util.parseBackendTimestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One row of `GET /api/course` (backend table `official_course`). Names may contain ruby markup. */
@Serializable
data class OfficialCourseDto(
    val id: Long,
    @SerialName("course_name") val courseName: String,
    @SerialName("course_description") val courseDescription: String = "",
    @SerialName("course_level") val courseLevel: String = "",
    @SerialName("course_language") val courseLanguage: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun OfficialCourseDto.toEntity() = OfficialCourseEntity(
    id = id,
    courseName = courseName,
    courseDescription = courseDescription,
    courseLevel = courseLevel,
    courseLanguage = courseLanguage,
    createdAt = parseBackendTimestamp(createdAt),
    updatedAt = parseBackendTimestamp(updatedAt),
)
