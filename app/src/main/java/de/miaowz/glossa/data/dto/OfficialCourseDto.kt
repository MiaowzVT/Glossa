package de.miaowz.glossa.data.dto

import de.miaowz.glossa.data.db.OfficialCourseEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class OfficialCourseDto(
    val id: Long,
    @SerialName("course_name") val courseName: String,
    @SerialName("course_description") val courseDescription: String = "",
    @SerialName("course_level") val courseLevel: String,
    @SerialName("course_language") val courseLanguage: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

fun OfficialCourseDto.toEntity() = OfficialCourseEntity(
    id = id,
    courseName = courseName,
    courseDescription = courseDescription,
    courseLevel = courseLevel,
    courseLanguage = courseLanguage,
    createdAt = Instant.parse(createdAt).toEpochMilli(),
    updatedAt = Instant.parse(updatedAt).toEpochMilli()
)