package de.miaowz.glossa.ui.community.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.miaowz.glossa.data.db.OfficialCourseEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialSetsScreen(
    viewModel: OfficialSetsViewModel = viewModel(factory = OfficialSetsViewModel .Factory),
    onCourseClick: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Courses") }) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                state.courses.isEmpty() && !state.isRefreshing -> EmptyState(state.error)
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    state.error?.let { msg ->
                        item {
                            Text(
                                msg,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    items(state.courses, key = { it.id }) { course ->
                        CourseItem(course, onClick = { onCourseClick(course.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseItem(course: OfficialCourseEntity, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(course.courseName) },
        supportingContent = {
            Text(course.courseDescription, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            Text("${course.courseLevel} · ${course.courseLanguage}",
                style = MaterialTheme.typography.labelMedium)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}

@Composable
private fun EmptyState(error: String?) {
    // verticalScroll keeps pull-to-refresh working on an empty screen
    Box(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Text(error ?: "No courses yet. Pull to refresh.", modifier = Modifier.padding(32.dp))
    }
}