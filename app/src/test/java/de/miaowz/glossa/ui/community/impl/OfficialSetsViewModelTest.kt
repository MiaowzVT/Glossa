package de.miaowz.glossa.ui.community.impl

import de.miaowz.glossa.data.model.OfficialCourse
import de.miaowz.glossa.network.BackendSamples
import de.miaowz.glossa.network.MockBackend
import de.miaowz.glossa.network.repository.FakeOfficialCourseDao
import de.miaowz.glossa.network.repository.OfficialCourseRepository
import de.miaowz.glossa.network.repository.courseEntity
import de.miaowz.glossa.ui.community.MainDispatcherRule
import de.miaowz.glossa.ui.community.SyncError
import de.miaowz.glossa.ui.community.impl.official_set.OfficialSetsUiState
import de.miaowz.glossa.ui.community.impl.official_set.OfficialSetsViewModel
import de.miaowz.glossa.ui.community.impl.official_set.courseSections
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OfficialSetsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val backend = MockBackend(mapOf("/api/course" to BackendSamples.COURSES))
    private val dao = FakeOfficialCourseDao()

    private fun viewModel() = OfficialSetsViewModel(OfficialCourseRepository(backend.courseApi, dao))

    /** First state after the initial sync (MockEngine answers on its own thread). */
    private suspend fun OfficialSetsViewModel.synced(): OfficialSetsUiState =
        uiState.first { !it.isLoading && !it.isRefreshing }

    @Test
    fun startsLoading() {
        assertTrue(OfficialSetsUiState().isLoading)
    }

    @Test
    fun loadsCoursesGroupedByLanguage() = runTest {
        val state = viewModel().synced()

        assertNull(state.error)
        assertEquals(listOf("Japanese", "Spanish"), state.sections.map { it.language })
        val course = state.sections[0].courses.single()
        assertEquals("{日本語|にほんご} N5", course.name)
        assertEquals("N5", course.level)
        assertEquals("Hiragana, Katakana and your first sentences.", course.description)
    }

    @Test
    fun offlineWithoutCacheShowsNetworkError() = runTest {
        backend.offline = true

        val state = viewModel().synced()

        assertTrue(state.isEmpty)
        assertEquals(SyncError.Network, state.error)
    }

    @Test
    fun serverErrorKeepsCachedCourses() = runTest {
        dao.upsertCourses(listOf(courseEntity(1, language = "German")))
        backend.status = HttpStatusCode.InternalServerError

        val state = viewModel().synced()

        assertEquals(SyncError.Server, state.error)
        assertEquals(listOf(1L), state.sections.flatMap { it.courses }.map { it.id })
    }

    @Test
    fun undecodableResponseIsInvalidData() = runTest {
        backend.routes["/api/course"] = "<html><body>Cannot GET /api/course</body></html>"

        assertEquals(SyncError.InvalidData, viewModel().synced().error)
    }

    @Test
    fun dismissErrorClearsIt() = runTest {
        backend.offline = true
        val viewModel = viewModel()
        viewModel.synced()

        viewModel.dismissError()

        assertNull(viewModel.uiState.first { it.error == null }.error)
    }

    @Test
    fun refreshAfterErrorRecovers() = runTest {
        backend.offline = true
        val viewModel = viewModel()
        assertEquals(SyncError.Network, viewModel.synced().error)

        backend.offline = false
        viewModel.refresh()

        val state = viewModel.uiState.first { !it.isRefreshing && !it.isEmpty }
        assertNull(state.error)
        assertEquals(2, state.sections.size)
    }

    @Test
    fun sectionsSortLanguagesAndNamesIgnoringReadings() {
        fun course(id: Long, name: String, language: String) =
            OfficialCourse(id, name, description = "", level = "", language = language, updatedAt = 0)

        val sections = courseSections(
            listOf(
                course(1, "Zebra", "spanish"),
                course(2, "{漢字|かんじ}", "Japanese"),
                course(3, "Basics", ""),
                course(4, "Alpha", "Spanish "),
                course(5, "{会話|かいわ}", "Japanese"),
            ),
        )

        assertEquals(listOf("Japanese", "Spanish", ""), sections.map { it.language })
        assertEquals(listOf(5L, 2L), sections[0].courses.map { it.id }) // 会話 < 漢字 by base text
        assertEquals(listOf(4L, 1L), sections[1].courses.map { it.id })
        assertEquals(listOf(3L), sections[2].courses.map { it.id })
    }
}
