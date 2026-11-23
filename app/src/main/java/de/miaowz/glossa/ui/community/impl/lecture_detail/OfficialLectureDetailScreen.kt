package de.miaowz.glossa.ui.community.impl.lecture_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.toRoute
import de.miaowz.glossa.R
import de.miaowz.glossa.data.model.OfficialLecture
import de.miaowz.glossa.network.repository.OfficialCourseLectureRepository
import de.miaowz.glossa.ruby.RubyText
import de.miaowz.glossa.ui.navigation.OfficialLectureDetailRoute
import de.miaowz.glossa.ui.officialCourseLectureRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface OfficialLectureDetailUiState {
    data object Loading : OfficialLectureDetailUiState
    data object NotFound : OfficialLectureDetailUiState
    data class Loaded(val lecture: OfficialLecture) : OfficialLectureDetailUiState
}

/** Shows a cached lecture; lectures are synced by the lecture list, so this works offline. */
class OfficialLectureDetailViewModel(
    lectureId: Long,
    repository: OfficialCourseLectureRepository,
) : ViewModel() {
    val uiState: StateFlow<OfficialLectureDetailUiState> = repository.observeLecture(lectureId)
        .map { lecture ->
            if (lecture == null) OfficialLectureDetailUiState.NotFound else OfficialLectureDetailUiState.Loaded(lecture)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OfficialLectureDetailUiState.Loading)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialLectureDetailScreen(
    onBack: () -> Unit,
    viewModel: OfficialLectureDetailViewModel = viewModel {
        OfficialLectureDetailViewModel(
            lectureId = createSavedStateHandle().toRoute<OfficialLectureDetailRoute>().lectureId,
            repository = officialCourseLectureRepository,
        )
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.community_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            OfficialLectureDetailUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            OfficialLectureDetailUiState.NotFound -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(stringResource(R.string.community_lecture_not_found))
            }
            is OfficialLectureDetailUiState.Loaded -> LectureContent(current.lecture, Modifier.padding(padding))
        }
    }
}

@Composable
private fun LectureContent(lecture: OfficialLecture, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RubyText(text = lecture.name, style = MaterialTheme.typography.headlineMedium)
        if (lecture.contents.isBlank) {
            Text(
                stringResource(R.string.community_lecture_empty_contents),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            RubyText(text = lecture.contents, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
