package de.miaowz.glossa.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.miaowz.glossa.data.GrammarRepository
import de.miaowz.glossa.data.VocabularyRepository
import de.miaowz.glossa.ui.grammarRepository
import de.miaowz.glossa.ui.vocabularyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Counts are `null` while loading. */
data class HomeUiState(
    val vocabularyCount: Int? = null,
    val grammarCount: Int? = null,
    /** Vocabulary cards due for repetition, including new ones. */
    val dueCount: Int? = null,
)

class HomeViewModel(grammarRepository: GrammarRepository, vocabularyRepository: VocabularyRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(
            vocabularyRepository.entryCount,
            grammarRepository.entryCount,
            vocabularyRepository.dueCount,
        ) { vocabulary, grammar, due ->
            HomeUiState(vocabularyCount = vocabulary, grammarCount = grammar, dueCount = due)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartReview: () -> Unit,
    viewModel: HomeViewModel = viewModel { HomeViewModel(grammarRepository, vocabularyRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Glossa") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReviewCard(state, onStartReview)
            CountCard(Icons.Outlined.Translate, "Vocabulary", state.vocabularyCount)
            CountCard(Icons.AutoMirrored.Outlined.MenuBook, "Grammar", state.grammarCount)
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
                    Text("Repetition", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            due == null -> ""
                            state.vocabularyCount == 0 -> "Add vocabulary to start practicing."
                            due == 0 -> "All caught up. Come back later!"
                            due == 1 -> "vocabulary card due"
                            else -> "vocabulary cards due"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(due?.toString() ?: "–", style = MaterialTheme.typography.displaySmall)
            }
            Button(onClick = onStartReview, enabled = (due ?: 0) > 0, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("Start repetition", Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun CountCard(icon: ImageVector, label: String, count: Int?) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (count == 1) "entry" else "entries",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(count?.toString() ?: "–", style = MaterialTheme.typography.displaySmall)
        }
    }
}
