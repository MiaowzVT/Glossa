package de.miaowz.glossa.ui.community.impl

import de.miaowz.glossa.network.BackendSamples
import de.miaowz.glossa.network.MockBackend
import de.miaowz.glossa.network.repository.FakeOfficialCourseDao
import de.miaowz.glossa.network.repository.FakeOfficialCourseLectureDao
import de.miaowz.glossa.network.repository.OfficialCourseLectureRepository
import de.miaowz.glossa.network.repository.OfficialCourseRepository
import de.miaowz.glossa.network.repository.courseEntity
import de.miaowz.glossa.network.repository.lectureEntity
import de.miaowz.glossa.richtext.RichText
import de.miaowz.glossa.ui.community.MainDispatcherRule
import de.miaowz.glossa.ui.community.SyncError
import de.miaowz.glossa.ui.community.impl.lecture_detail.OfficialLectureDetailUiState
import de.miaowz.glossa.ui.community.impl.lecture_detail.OfficialLectureDetailViewModel
import de.miaowz.glossa.ui.community.impl.lecture_list.OfficialSetLectureListUiState
import de.miaowz.glossa.ui.community.impl.lecture_list.OfficialSetLectureListViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OfficialSetLectureListViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val backend = MockBackend(
        mapOf(
            "/api/course/2" to BackendSamples.COURSE_AS_ARRAY,
            "/api/course/2/lectures" to BackendSamples.LECTURES,
        ),
    )
    private val courseDao = FakeOfficialCourseDao()
    private val lectureDao = FakeOfficialCourseLectureDao()
    private val lectureRepository = OfficialCourseLectureRepository(backend.lectureApi, lectureDao)

    private fun viewModel(courseId: Long = 2) = OfficialSetLectureListViewModel(
        courseId = courseId,
        courseRepository = OfficialCourseRepository(backend.courseApi, courseDao),
        lectureRepository = lectureRepository,
    )

    private suspend fun OfficialSetLectureListViewModel.synced(): OfficialSetLectureListUiState =
        uiState.first { !it.isLoading && !it.isRefreshing }

    @Test
    fun loadsCourseHeaderAndItsLectures() = runTest {
        val state = viewModel().synced()

        assertNull(state.error)
        assertEquals("{日本語|にほんご} N5", state.course?.name)
        assertEquals("Japanese", state.course?.language)
        assertEquals(listOf(5L, 6L), state.lectures.map { it.id })
        assertEquals(RichText("一、二、三"), state.lectures[1].contents)
        assertEquals(listOf("/api/course/2", "/api/course/2/lectures"), backend.requests)
    }

    @Test
    fun courseRemovedOnBackendIsNotFound() = runTest {
        courseDao.upsertCourses(listOf(courseEntity(2)))
        backend.routes["/api/course/2"] = "[]"

        val state = viewModel().synced()

        assertNull(state.course)
        assertNull(state.error)
        assertEquals(listOf("/api/course/2"), backend.requests)
    }

    @Test
    fun offlineShowsCachedLecturesWithError() = runTest {
        courseDao.upsertCourses(listOf(courseEntity(2)))
        lectureDao.upsertLectures(listOf(lectureEntity(10, courseId = 2), lectureEntity(11, courseId = 3)))
        backend.offline = true

        val state = viewModel().synced()

        assertEquals(SyncError.Network, state.error)
        assertEquals(2L, state.course?.id)
        assertEquals(listOf(10L), state.lectures.map { it.id })
    }

    @Test
    fun offlineWithoutCacheHasNoCourse() = runTest {
        backend.offline = true

        val state = viewModel().synced()

        assertNull(state.course)
        assertTrue(state.lectures.isEmpty())
        assertEquals(SyncError.Network, state.error)
    }

    @Test
    fun lectureDetailShowsDecodedContents() = runTest {
        viewModel().synced()

        val state = OfficialLectureDetailViewModel(5, lectureRepository).uiState
            .first { it != OfficialLectureDetailUiState.Loading }

        val lecture = (state as OfficialLectureDetailUiState.Loaded).lecture
        assertEquals("{挨拶|あいさつ}", lecture.name)
        assertEquals("{今日|こんにち}は means hello.", lecture.contents.text)
        assertEquals(2, lecture.contents.spans.size)
    }

    @Test
    fun lectureDetailUnknownIdIsNotFound() = runTest {
        val state = OfficialLectureDetailViewModel(404, lectureRepository).uiState
            .first { it != OfficialLectureDetailUiState.Loading }

        assertEquals(OfficialLectureDetailUiState.NotFound, state)
    }
}
