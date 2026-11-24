package de.miaowz.glossa.network.repository

import de.miaowz.glossa.network.BackendSamples
import de.miaowz.glossa.network.MockBackend
import de.miaowz.glossa.richtext.RichSpan
import de.miaowz.glossa.richtext.RichText
import de.miaowz.glossa.richtext.SpanFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfficialCourseLectureRepositoryTest {

    private val backend = MockBackend(mapOf("/api/course/2/lectures" to BackendSamples.LECTURES))
    private val dao = FakeOfficialCourseLectureDao()
    private val repository = OfficialCourseLectureRepository(backend.lectureApi, dao)

    @Test
    fun refreshLoadsLecturesOfTheCourse() = runBlocking {
        repository.refreshLectures(2)

        val lectures = repository.observeLectures(2).first()
        assertEquals(listOf(5L, 6L), lectures.map { it.id })
        assertEquals("{挨拶|あいさつ}", lectures[0].name)
        assertEquals(listOf("/api/course/2/lectures"), backend.requests)
    }

    @Test
    fun contentsAreDecodedRichTextWithRubyMarkupKept() = runBlocking {
        repository.refreshLectures(2)

        val lecture = repository.observeLecture(5).first()!!
        assertEquals(
            RichText(
                "{今日|こんにち}は means hello.",
                listOf(
                    RichSpan(0, 10, SpanFormat.Bold),
                    RichSpan(11, 16, SpanFormat.TextColor(-1739980)),
                ),
            ),
            lecture.contents,
        )
    }

    @Test
    fun plainTextContentsFallBackToPlainRichText() = runBlocking {
        dao.upsertLectures(listOf(lectureEntity(1, courseId = 4, contents = "Just text")))

        assertEquals(RichText("Just text"), repository.observeLecture(1).first()?.contents)
    }

    @Test
    fun refreshReplacesOnlyTheLecturesOfThatCourse() = runBlocking {
        dao.upsertLectures(listOf(lectureEntity(1, courseId = 2), lectureEntity(2, courseId = 3)))

        repository.refreshLectures(2)

        assertNull(repository.observeLecture(1).first())
        assertEquals(listOf(2L), repository.observeLectures(3).first().map { it.id })
    }

    @Test
    fun emptyResponseClearsTheCourse() = runBlocking {
        dao.upsertLectures(listOf(lectureEntity(1, courseId = 7)))
        backend.routes["/api/course/7/lectures"] = "[]"

        repository.refreshLectures(7)

        assertEquals(emptyList<Any>(), repository.observeLectures(7).first())
    }
}
