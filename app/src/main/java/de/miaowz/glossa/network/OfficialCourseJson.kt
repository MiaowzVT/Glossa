package de.miaowz.glossa.network

import de.miaowz.glossa.data.dto.OfficialCourseDto
import de.miaowz.glossa.data.dto.OfficialCourseLectureDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Decodes the official-course endpoints from the raw response body.
 *
 * The backend does not reliably send `Content-Type: application/json`, so responses are read as
 * text and decoded here instead of through Ktor's content negotiation. Throws
 * `kotlinx.serialization.SerializationException` (an `IllegalArgumentException`) for malformed bodies.
 */
object OfficialCourseJson {
    private val json = Json {
        ignoreUnknownKeys = true
        // `null` in a non-nullable field with a default (e.g. a NULL description) uses the default.
        coerceInputValues = true
        isLenient = true
    }

    fun decodeCourses(body: String): List<OfficialCourseDto> =
        json.decodeFromString(ListSerializer(OfficialCourseDto.serializer()), body)

    /**
     * `GET /api/course/:id` returns the course wrapped in an array (backend bug: `findById` returns
     * all rows). Accepts both shapes; `null` when the course does not exist (`[]`, `null` or empty body).
     */
    fun decodeCourse(body: String): OfficialCourseDto? {
        if (body.isBlank()) return null
        return when (val element = json.parseToJsonElement(body)) {
            is JsonArray -> element.firstOrNull()?.let { json.decodeFromJsonElement(OfficialCourseDto.serializer(), it) }
            is JsonObject -> json.decodeFromJsonElement(OfficialCourseDto.serializer(), element)
            JsonNull -> null
            else -> throw IllegalArgumentException("Unexpected course JSON: $element")
        }
    }

    fun decodeLectures(body: String): List<OfficialCourseLectureDto> =
        json.decodeFromString(ListSerializer(OfficialCourseLectureDto.serializer()), body)
}
