package de.miaowz.glossa.ui.community.impl.lecture_list

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import de.miaowz.glossa.GlossaApplication
import de.miaowz.glossa.data.db.OfficialCourseEntity
import de.miaowz.glossa.data.db.OfficialCourseLectureEntity
import de.miaowz.glossa.network.repository.OfficialCourseLectureRepository
import de.miaowz.glossa.network.repository.OfficialCourseRepository
import de.miaowz.glossa.ui.navigation.OfficialSetLectureListRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OfficialSetLectureListUiState(
    val entryId: Long,
    val lectures: List<OfficialCourseLectureEntity> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class OfficialSetLectureListViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: OfficialCourseLectureRepository
) : ViewModel() {

    private val entryId = savedStateHandle.toRoute<OfficialSetLectureListRoute>().entryId
    private val isRefreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<OfficialSetLectureListUiState> =
        combine(repository.lectures, isRefreshing, error) { lectures, refreshing, err ->
            OfficialSetLectureListUiState(entryId, lectures, refreshing, err)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
            OfficialSetLectureListUiState(entryId))

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            error.value = null
            try {
                repository.refresh()
            } catch (e: Exception) {
                Log.e("Sync", "Refresh failed", e)
                error.value = "Couldn't sync courses - are you perhaps offline?"
            } finally {
                isRefreshing.value = false
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GlossaApplication
                OfficialSetLectureListViewModel(createSavedStateHandle(), app.officialCourseLectureRepository)
            }
        }
    }
}