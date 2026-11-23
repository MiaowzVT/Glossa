package de.miaowz.glossa.ui.community.impl.official_set

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
import de.miaowz.glossa.network.repository.OfficialCourseRepository
import de.miaowz.glossa.ui.navigation.OfficialSetLectureListRoute
import de.miaowz.glossa.ui.navigation.VocabularyEditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OfficialSetsUiState(
    val courses: List<OfficialCourseEntity> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class OfficialSetsViewModel(
    private val repository: OfficialCourseRepository
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<OfficialSetsUiState> =
        combine(repository.courses, isRefreshing, error) { courses, refreshing, err ->
            OfficialSetsUiState(courses, refreshing, err)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OfficialSetsUiState())

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
                OfficialSetsViewModel(app.officialCourseRepository)
            }
        }
    }
}