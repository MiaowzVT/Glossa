package de.miaowz.glossa.ui.community.impl.lecture_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.miaowz.glossa.data.model.OfficialCourse
import de.miaowz.glossa.data.model.OfficialLecture
import de.miaowz.glossa.network.repository.OfficialCourseLectureRepository
import de.miaowz.glossa.network.repository.OfficialCourseRepository
import de.miaowz.glossa.ui.community.SyncError
import de.miaowz.glossa.ui.community.log
import de.miaowz.glossa.ui.community.syncErrorOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OfficialSetLectureListUiState(
    /** The cached course has not been read yet. */
    val isLoading: Boolean = true,
    /** `null` when the course is not cached (yet) or was removed on the backend. */
    val course: OfficialCourse? = null,
    val lectures: List<OfficialLecture> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: SyncError? = null,
)

/** Course header and lectures of [courseId], from the cache; [refresh] syncs both from the backend. */
class OfficialSetLectureListViewModel(
    private val courseId: Long,
    private val courseRepository: OfficialCourseRepository,
    private val lectureRepository: OfficialCourseLectureRepository,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<SyncError?>(null)

    val uiState: StateFlow<OfficialSetLectureListUiState> = combine(
        courseRepository.observeCourse(courseId),
        lectureRepository.observeLectures(courseId),
        isRefreshing,
        error,
    ) { course, lectures, refreshing, err ->
        OfficialSetLectureListUiState(
            isLoading = false,
            course = course,
            lectures = lectures,
            isRefreshing = refreshing,
            error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OfficialSetLectureListUiState())

    init {
        refresh()
    }

    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        error.value = null
        viewModelScope.launch {
            try {
                // A course the backend no longer has is dropped from the cache; its lectures are moot.
                if (courseRepository.refreshCourse(courseId)) {
                    lectureRepository.refreshLectures(courseId)
                }
            } catch (e: Exception) {
                // syncErrorOf rethrows cancellation, so only real failures are logged.
                error.value = syncErrorOf(e).also { it.log(e, "Syncing course $courseId") }
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun dismissError() {
        error.value = null
    }
}
