package de.miaowz.glossa.ui.community.impl.lecture_list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.toRoute
import de.miaowz.glossa.R
import de.miaowz.glossa.data.model.OfficialCourse
import de.miaowz.glossa.data.model.OfficialLecture
import de.miaowz.glossa.ruby.RubyText
import de.miaowz.glossa.ui.community.CommunityErrorMessage
import de.miaowz.glossa.ui.community.CommunityMessage
import de.miaowz.glossa.ui.community.SyncErrorCard
import de.miaowz.glossa.ui.navigation.OfficialSetLectureListRoute
import de.miaowz.glossa.ui.officialCourseLectureRepository
import de.miaowz.glossa.ui.officialCourseRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialSetLectureListScreen(
    onBack: () -> Unit,
    onOpenLecture: (Long) -> Unit,
    viewModel: OfficialSetLectureListViewModel = viewModel {
        OfficialSetLectureListViewModel(
            courseId = createSavedStateHandle().toRoute<OfficialSetLectureListRoute>().entryId,
            courseRepository = officialCourseRepository,
            lectureRepository = officialCourseLectureRepository,
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
        val course = state.course
        PullToRefreshBox(
            isRefreshing = state.isRefreshing && course != null,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val error = state.error
            when {
                state.isLoading || (course == null && state.isRefreshing) ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                course == null && error != null ->
                    CommunityErrorMessage(stringResource(R.string.community_lectures_error_title), error)
                course == null -> CommunityMessage(
                    icon = Icons.Outlined.School,
                    title = stringResource(R.string.community_course_not_found_title),
                    text = stringResource(R.string.community_course_not_found_text),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (error != null) {
                        item(key = "error") { SyncErrorCard(error, onDismiss = viewModel::dismissError) }
                    }
                    item(key = "course") {
                        // No "0 lectures" while the first sync of this course is still running.
                        val count = state.lectures.size.takeUnless { it == 0 && state.isRefreshing }
                        CourseHeader(course, count)
                    }
                    item(key = "lectures-header") {
                        Text(
                            text = stringResource(R.string.community_lectures_header),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                        )
                    }
                    if (state.lectures.isEmpty() && !state.isRefreshing) {
                        item(key = "no-lectures") { NoLectures() }
                    }
                    itemsIndexed(state.lectures, key = { _, lecture -> lecture.id }) { index, lecture ->
                        LectureCard(index + 1, lecture, onClick = { onOpenLecture(lecture.id) })
                    }
                }
            }
        }
    }
}

/** Same layout as the vocabulary detail screen's header. */
@Composable
private fun CourseHeader(course: OfficialCourse, lectureCount: Int?) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RubyText(text = course.name, style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (course.language.isNotBlank()) AssistChip(onClick = {}, label = { Text(course.language) })
            if (course.level.isNotBlank()) AssistChip(onClick = {}, label = { Text(course.level) })
        }
        if (course.description.isNotBlank()) {
            RubyText(text = course.description, style = MaterialTheme.typography.bodyLarge)
        }
        if (lectureCount != null) {
            Text(
                text = pluralStringResource(R.plurals.community_lecture_count, lectureCount, lectureCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoLectures() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.community_lectures_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.community_lectures_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LectureCard(number: Int, lecture: OfficialLecture, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.community_lecture_number, number),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RubyText(
                text = lecture.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
        }
    }
}
