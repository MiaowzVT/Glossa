package de.miaowz.glossa.network

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialCourseJsonTest {

    @Test
    fun decodesCourseListWithAllFields() {
        val courses = OfficialCourseJson.decodeCourses(BackendSamples.COURSES)

        assertEquals(listOf(2L, 1L), courses.map { it.id })
        val japanese = courses[0]
        assertEquals("{日本語|にほんご} N5", japanese.courseName)
        assertEquals("Hiragana, Katakana and your first sentences.", japanese.courseDescription)
        assertEquals("N5", japanese.courseLevel)
        assertEquals("Japanese", japanese.courseLanguage)
        assertEquals("2026-11-21T08:00:00.000Z", japanese.updatedAt)
    }

    @Test
    fun decodesEmptyCourseList() {
        assertTrue(OfficialCourseJson.decodeCourses("[]").isEmpty())
    }

    @Test
    fun singleCourseAcceptsArrayAndObject() {
        val fromArray = OfficialCourseJson.decodeCourse(BackendSamples.COURSE_AS_ARRAY)
        val fromObject = OfficialCourseJson.decodeCourse(BackendSamples.COURSE_AS_OBJECT)

        assertEquals(2L, fromArray?.id)
        assertEquals(fromArray, fromObject)
    }

    @Test
    fun singleCourseIsNullWhenBackendFindsNothing() {
        // Unknown ids return 200 with an empty array.
        assertNull(OfficialCourseJson.decodeCourse("[]"))
        assertNull(OfficialCourseJson.decodeCourse("null"))
        assertNull(OfficialCourseJson.decodeCourse(""))
    }

    @Test
    fun decodesLecturesKeepingRichTextJsonAsString() {
        val lectures = OfficialCourseJson.decodeLectures(BackendSamples.LECTURES)

        assertEquals(2, lectures.size)
        assertEquals(2L, lectures[0].courseId)
        assertEquals("{挨拶|あいさつ}", lectures[0].lectureName)
        assertTrue(lectures[0].lectureContents.startsWith("{\"text\":\"{今日|こんにち}は"))
        assertNull(lectures[1].createdAt)
        assertNull(lectures[1].updatedAt)
    }

    @Test
    fun ignoresUnknownFieldsAndNullDescription() {
        val body = """[{"id":3,"course_name":"Deutsch","course_description":null,"course_level":"B1",
            "course_language":"German","author":"admin","created_at":"2026-11-17T14:09:42.000Z",
            "updated_at":"2026-11-17T14:09:42.000Z"}]"""

        val course = OfficialCourseJson.decodeCourses(body).single()

        assertEquals("", course.courseDescription)
        assertEquals("B1", course.courseLevel)
    }

    @Test
    fun malformedBodyThrowsSerializationException() {
        assertThrows(SerializationException::class.java) { OfficialCourseJson.decodeCourses("<html>Error</html>") }
        assertThrows(SerializationException::class.java) { OfficialCourseJson.decodeCourses("""{"id":1}""") }
    }
}
