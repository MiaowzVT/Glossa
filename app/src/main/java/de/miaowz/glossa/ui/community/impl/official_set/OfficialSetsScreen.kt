
package de.miaowz.glossa.ui.community.impl.official_set

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.miaowz.glossa.data.db.OfficialCourseEntity

private const val TAG = "Glossa::OfficialSetsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialSetsScreen(
    viewModel: OfficialSetsViewModel = viewModel(
        factory = OfficialSetsViewModel.Factory
    ),
    onCourseClick: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Courses") }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                state.courses.isEmpty() && !state.isRefreshing -> {
                    EmptyState(state.error)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.error?.let { msg ->
                            item {
                                ClosableCard(
                                    title = "An error occurred",
                                    body = msg
                                )
                            }
                        }

                        items(
                            items = state.courses,
                            key = { it.id }
                        ) { course ->
                            CourseItem(
                                course = course,
                                onClick = {
                                    onCourseClick(course.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseItem(
    course: OfficialCourseEntity,
    onClick: () -> Unit
) {
    CourseCardContent(
        name = course.courseName,
        description = course.courseDescription,
        level = course.courseLevel,
        language = course.courseLanguage,
        onClick = onClick
    )
}

@Composable
private fun CourseCardContent(
    name: String,
    description: String,
    level: String,
    language: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (level.isNotBlank() || language.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (level.isNotBlank()) {
                        AssistChip(
                            onClick = onClick,
                            label = { Text(level) }
                        )
                    }

                    if (language.isNotBlank()) {
                        AssistChip(
                            onClick = onClick,
                            label = { Text(language) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(error: String?) {
    // verticalScroll keeps pull-to-refresh working on an empty screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        if (error != null) {
            Log.e(TAG, "Error: $error")

            ClosableCard(
                title = "An error occurred",
                body = error
            )
        } else {
            Log.i(TAG, "No courses found")

            Text(
                text = "No courses yet. Pull to refresh.",
                modifier = Modifier.padding(32.dp)
            )
        }
    }
}

@Composable
fun ClosableCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    var isVisible by remember {
        mutableStateOf(true)
    }

    Log.i(
        TAG,
        "Building ClosableCard with properties title=$title, body=$body"
    )

    AnimatedVisibility(
        visible = isVisible,
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        top = 8.dp,
                        end = 8.dp,
                        bottom = 16.dp
                    ),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                IconButton(
                    onClick = {
                        isVisible = false
                        onDismiss()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Card",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "Official Course Card"
)
@Composable
private fun CourseCardPreview() {
    MaterialTheme {
        CourseCardContent(
            name = "Introduction to Japanese",
            description = "Learn the basics, greetings and simple expressions.",
            level = "N5",
            language = "Japanese",
            onClick = {}
        )
    }
}
