package de.miaowz.glossa.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReviewCard(state, onStartReview)
        }
    }
}

@Composable
private fun ReviewCard(state: HomeUiState, onStartReview: () -> Unit) {
    val due = state.dueCount
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.repitition), style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            due == null -> ""
                            due == 0 -> stringResource(R.string.due_count_0)
                            due == 1 -> stringResource(R.string.due_count_1)
                            else -> stringResource(R.string.due_count_n, due)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(due?.toString() ?: "–", style = MaterialTheme.typography.displaySmall)
            }
            Button(onClick = onStartReview, enabled = (due ?: 0) > 0, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.start_repitition), Modifier.padding(start = 8.dp))
            }
        }
    }
}