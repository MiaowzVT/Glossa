package de.miaowz.glossa.data.dto

import org.junit.Assert.assertEquals
import org.junit.Test

class OfficialCourseDtoTest {

    @Test
    fun parsesMysqlIsoTimestamps() {
        assertEquals(1_794_924_582_000L, parseBackendTimestamp("2026-11-17T14:09:42.000Z"))
        assertEquals(1_794_924_582_000L, parseBackendTimestamp("2026-11-17T14:09:42Z"))
    }

    @Test
    fun parsesSqlDateTimeAsUtc() {
        assertEquals(1_794_924_582_000L, parseBackendTimestamp("2026-11-17 14:09:42"))
    }

    @Test
    fun missingOrInvalidTimestampsBecomeZero() {
        assertEquals(0L, parseBackendTimestamp(null))
        assertEquals(0L, parseBackendTimestamp(""))
        assertEquals(0L, parseBackendTimestamp("yesterday"))
    }

    @Test
    fun courseMapsToEntity() {
        val entity = OfficialCourseDto(
            id = 7,
            courseName = "{漢字|かんじ}",
            courseDescription = "Kanji basics",
            courseLevel = "N4",
            courseLanguage = "Japanese",
            createdAt = "2026-11-17T14:09:42.000Z",
            updatedAt = null,
        ).toEntity()

        assertEquals(7L, entity.id)
        assertEquals("{漢字|かんじ}", entity.courseName)
        assertEquals("Kanji basics", entity.courseDescription)
        assertEquals("N4", entity.courseLevel)
        assertEquals("Japanese", entity.courseLanguage)
        assertEquals(1_794_924_582_000L, entity.createdAt)
        assertEquals(0L, entity.updatedAt)
    }

    @Test
    fun lectureMapsToEntityKeepingRawContents() {
        val entity = OfficialCourseLectureDto(
            id = 5,
            courseId = 2,
            lectureName = "Numbers",
            lectureContents = """{"text":"一"}""",
            createdAt = null,
            updatedAt = "2026-11-17T14:09:42.000Z",
        ).toEntity()

        assertEquals(2L, entity.courseId)
        assertEquals("""{"text":"一"}""", entity.lectureContents)
        assertEquals(1_794_924_582_000L, entity.updatedAt)
    }
}
