package de.miaowz.glossa.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.miaowz.glossa.R
import de.miaowz.glossa.data.VocabularyRepository
import de.miaowz.glossa.ui.vocabularyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Counts are `null` while loading. */
data class HomeUiState(
    /** Vocabulary cards due for repetition, including new ones. */
    val dueCount: Int? = null,
)

class HomeViewModel(vocabularyRepository: VocabularyRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = vocabularyRepository.dueCount
        .map { due -> HomeUiState(due) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartReview: () -> Unit,
    viewModel: HomeViewModel = viewModel { HomeViewModel(vocabularyRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReviewCard(state = state, onStartReview = onStartReview)
        }
    }
}

@Composable
private fun ReviewCard(state: HomeUiState, onStartReview: () -> Unit) {
    val due = state.dueCount
    val description = when {
        due == null -> ""
        due == 0 -> stringResource(R.string.due_count_0)
        due == 1 -> stringResource(R.string.due_count_1)
        else -> stringResource(R.string.due_count_n, due)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.repitition),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                text = due?.toString() ?: "–",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Button(
                onClick = onStartReview,
                enabled = (due ?: 0) > 0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(
                    text = stringResource(R.string.start_repitition),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Preview(name = "12 due", showBackground = true)
@Composable
private fun ReviewCardWithDuePreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp)) {
            ReviewCard(state = HomeUiState(dueCount = 12), onStartReview = {})
        }
    }
}

@Preview(name = "0 due", showBackground = true)
@Composable
private fun ReviewCardEmptyPreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp)) {
            ReviewCard(state = HomeUiState(dueCount = 0), onStartReview = {})
        }
    }
}
