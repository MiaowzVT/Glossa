package de.miaowz.glossa.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.toRoute
import de.miaowz.glossa.data.GrammarRepository
import de.miaowz.glossa.data.model.GrammarEntry
import de.miaowz.glossa.ruby.RubyText
import de.miaowz.glossa.ui.grammarRepository
import de.miaowz.glossa.ui.navigation.EntryDetailRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object NotFound : DetailUiState
    data class Loaded(val entry: GrammarEntry) : DetailUiState
}

class GrammarDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: GrammarRepository,
) : ViewModel() {
    private val entryId = savedStateHandle.toRoute<EntryDetailRoute>().entryId

    val uiState: StateFlow<DetailUiState> = repository.observeEntry(entryId)
        .map { entry -> if (entry == null) DetailUiState.NotFound else DetailUiState.Loaded(entry) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun delete() {
        viewModelScope.launch {
            repository.delete(entryId)
            _deleted.value = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: GrammarDetailViewModel = viewModel {
        GrammarDetailViewModel(createSavedStateHandle(), grammarRepository)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val loaded = state as? DetailUiState.Loaded
                    if (loaded != null) {
                        IconButton(onClick = { onEdit(loaded.entry.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            DetailUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            // Also shown briefly after deleting, before navigating back.
            DetailUiState.NotFound -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("This entry no longer exists.")
            }
            is DetailUiState.Loaded -> EntryContent(current.entry, Modifier.padding(padding))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete entry?") },
            text = { Text("This entry and its example sentences will be deleted permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EntryContent(entry: GrammarEntry, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RubyText(text = entry.title, style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text(entry.language.name) })
            entry.category?.let { AssistChip(onClick = {}, label = { Text(it.name) }) }
        }
        if (!entry.description.isBlank) {
            RubyText(text = entry.description, style = MaterialTheme.typography.bodyLarge)
        }
        if (entry.examples.isNotEmpty()) {
            HorizontalDivider()
            Text(
                "Examples",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            entry.examples.forEach { example ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RubyText(text = example.sentence, style = MaterialTheme.typography.bodyLarge)
                        if (!example.translation.isBlank) {
                            RubyText(
                                text = example.translation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
