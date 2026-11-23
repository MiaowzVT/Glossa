package de.miaowz.glossa.ui.community.impl.official_set

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.miaowz.glossa.data.model.OfficialCourse
import de.miaowz.glossa.network.repository.OfficialCourseRepository
import de.miaowz.glossa.ruby.RubyMarkup
import de.miaowz.glossa.ui.community.SyncError
import de.miaowz.glossa.ui.community.syncErrorOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Courses of one language; [language] is `""` for courses without one. */
data class CourseSection(
    val language: String,
    val courses: List<OfficialCourse>,
)

data class OfficialSetsUiState(
    /** The cached courses have not been read yet. */
    val isLoading: Boolean = true,
    val sections: List<CourseSection> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: SyncError? = null,
) {
    val isEmpty: Boolean get() = sections.isEmpty()
}

/**
 * Groups courses like the libraries: one section per language (matched and sorted
 * case-insensitively, courses without a language last), courses by name without readings.
 * The language is free text from the admin panel, so "Japanese" and "japanese " are one section.
 */
fun courseSections(courses: List<OfficialCourse>): List<CourseSection> = courses
    .groupBy { it.language.trim().lowercase() }
    .toSortedMap(compareBy<String> { it.isEmpty() }.thenBy { it })
    .map { (_, items) ->
        val sorted = items.sortedBy { RubyMarkup.stripToBase(it.name).lowercase() }
        CourseSection(language = sorted.first().language.trim(), courses = sorted)
    }

class OfficialSetsViewModel(
    private val repository: OfficialCourseRepository,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<SyncError?>(null)

    val uiState: StateFlow<OfficialSetsUiState> =
        combine(repository.courses, isRefreshing, error) { courses, refreshing, err ->
            OfficialSetsUiState(
                isLoading = false,
                sections = courseSections(courses),
                isRefreshing = refreshing,
                error = err,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OfficialSetsUiState())

    init {
        refresh()
    }

    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        error.value = null
        viewModelScope.launch {
            try {
                repository.refresh()
            } catch (e: Exception) {
                error.value = syncErrorOf(e)
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun dismissError() {
        error.value = null
    }
}
