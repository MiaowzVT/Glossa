package de.miaowz.glossa.ui.community.impl.official_set

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.miaowz.glossa.R
import de.miaowz.glossa.data.model.OfficialCourse
import de.miaowz.glossa.ruby.RubyText
import de.miaowz.glossa.ui.community.CommunityErrorMessage
import de.miaowz.glossa.ui.community.CommunityMessage
import de.miaowz.glossa.ui.community.SyncError
import de.miaowz.glossa.ui.community.SyncErrorCard
import de.miaowz.glossa.ui.officialCourseRepository
import de.miaowz.glossa.ui.theme.GlossaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialSetsScreen(
    onBack: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    viewModel: OfficialSetsViewModel = viewModel { OfficialSetsViewModel(officialCourseRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.community_official_sets_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.community_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        PullToRefreshBox(
            // Keep existing refresh behavior, including the full-screen initial loader.
            isRefreshing = state.isRefreshing && !state.isEmpty,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val error = state.error
            when {
                state.isLoading || (state.isEmpty && state.isRefreshing) ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                state.isEmpty && error != null ->
                    CommunityErrorMessage(
                        stringResource(R.string.community_official_sets_error_title),
                        error,
                    )
                state.isEmpty -> CommunityMessage(
                    icon = Icons.Outlined.School,
                    title = stringResource(R.string.community_official_sets_empty_title),
                    text = stringResource(R.string.community_official_sets_empty_text),
                )
                else -> CourseList(state, error, viewModel::dismissError, onOpenCourse)
            }
        }
    }
}

@Composable
private fun CourseList(
    state: OfficialSetsUiState,
    error: SyncError?,
    onDismissError: () -> Unit,
    onOpenCourse: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (error != null) {
            item(key = "error") { SyncErrorCard(error, onDismiss = onDismissError) }
        }
        state.sections.forEach { section ->
            item(key = "language-${section.language}") {
                Text(
                    text = section.language.ifEmpty {
                        stringResource(R.string.community_course_language_unknown)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                )
            }
            items(section.courses, key = { it.id }) { course ->
                CourseCard(course, onClick = { onOpenCourse(course.id) })
            }
        }
    }
}

@Composable
private fun CourseCard(course: OfficialCourse, onClick: () -> Unit) {
    CourseCardContent(
        level = course.level,
        description = {
            if (course.description.isNotBlank()) {
                RubyText(
                    text = course.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                )
            }
        },
        name = {
            RubyText(
                text = course.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
        },
        onClick = onClick,
    )
}

/** Shared card design with the vocabulary library; content slots preserve RubyText. */
@Composable
private fun CourseCardContent(
    level: String,
    description: @Composable () -> Unit,
    name: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Box(modifier = Modifier.weight(1f)) { name() }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            description()

            if (level.isNotBlank()) {
                AssistChip(
                    onClick = onClick,
                    label = { Text(level) },
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Official course card")
@Composable
private fun CourseCardPreview() {
    GlossaTheme(dynamicColor = false) {
        CourseCardContent(
            level = "N5",
            description = {
                Text(
                    text = "Learn the basics, greetings and simple expressions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            name = {
                Text(
                    text = "Introduction to Japanese",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            onClick = {},
        )
    }
}
