package de.miaowz.glossa.data.dto

import de.miaowz.glossa.data.db.OfficialCourseEntity
import de.miaowz.glossa.data.db.OfficialCourseLectureEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class OfficialCourseLectureDto(
    val id: Long,
    @SerialName("course_id") val courseId: Long,
    @SerialName("lecture_name") val lectureName: String = "",
    @SerialName("lecture_contents") val lectureContents: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

fun OfficialCourseLectureDto.toEntity() = OfficialCourseLectureEntity(
    id = id,
    courseId = courseId,
    lectureName = lectureName,
    lectureContents = lectureContents,
    createdAt = Instant.parse(createdAt).toEpochMilli(),
    updatedAt = Instant.parse(updatedAt).toEpochMilli()
)