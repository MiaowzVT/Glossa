package de.miaowz.glossa.network.repository

import de.miaowz.glossa.network.BackendSamples
import de.miaowz.glossa.network.MockBackend
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialCourseRepositoryTest {

    private val backend = MockBackend(mapOf("/api/course" to BackendSamples.COURSES))
    private val dao = FakeOfficialCourseDao()
    private val repository = OfficialCourseRepository(backend.courseApi, dao)

    @Test
    fun refreshStoresAllCoursesWithAllFields() = runBlocking {
        repository.refresh()

        val courses = repository.courses.first()
        assertEquals(setOf(1L, 2L), courses.map { it.id }.toSet())
        val japanese = courses.single { it.id == 2L }
        assertEquals("{日本語|にほんご} N5", japanese.name)
        assertEquals("Hiragana, Katakana and your first sentences.", japanese.description)
        assertEquals("N5", japanese.level)
        assertEquals("Japanese", japanese.language)
    }

    @Test
    fun refreshReplacesCoursesRemovedOnTheBackend() = runBlocking {
        dao.upsertCourses(listOf(courseEntity(99)))

        repository.refresh()

        assertNull(repository.observeCourse(99).first())
    }

    @Test
    fun failedRefreshKeepsCachedCourses() = runBlocking {
        dao.upsertCourses(listOf(courseEntity(1)))
        backend.offline = true

        assertThrows(Exception::class.java) { runBlocking { repository.refresh() } }
        assertEquals(listOf(1L), repository.courses.first().map { it.id })
    }

    @Test
    fun refreshCourseUsesArrayResponseOfSingleCourseEndpoint() = runBlocking {
        backend.routes["/api/course/2"] = BackendSamples.COURSE_AS_ARRAY

        assertTrue(repository.refreshCourse(2))
        assertEquals("N5", repository.observeCourse(2).first()?.level)
        assertEquals(listOf("/api/course/2"), backend.requests)
    }

    @Test
    fun refreshCourseDropsCourseTheBackendNoLongerHas() = runBlocking {
        dao.upsertCourses(listOf(courseEntity(3)))
        backend.routes["/api/course/3"] = "[]"

        assertFalse(repository.refreshCourse(3))
        assertNull(repository.observeCourse(3).first())
    }
}
