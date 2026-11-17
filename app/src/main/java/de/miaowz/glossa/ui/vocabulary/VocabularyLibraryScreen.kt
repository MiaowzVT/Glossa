package de.miaowz.glossa.ui.vocabulary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.miaowz.glossa.data.model.VocabularyEntry
import de.miaowz.glossa.ruby.RubyText
import de.miaowz.glossa.ui.common.EmptyLibrary
import de.miaowz.glossa.ui.common.LibraryList
import de.miaowz.glossa.ui.vocabularyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyLibraryScreen(
    onAddEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: VocabularyLibraryViewModel = viewModel { VocabularyLibraryViewModel(vocabularyRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(title = { Text("Vocabulary") }, scrollBehavior = scrollBehavior)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Icon(Icons.Filled.Add, contentDescription = "Add vocabulary entry")
            }
        },
    ) { padding ->
        LibraryList(
            state = state,
            padding = padding,
            searchPlaceholder = "Search vocabulary",
            onQueryChange = viewModel::setQuery,
            onSelectLanguage = viewModel::selectLanguage,
            onSelectCategory = viewModel::selectCategory,
            key = { it.id },
            empty = { modifier ->
                EmptyLibrary(
                    icon = Icons.Outlined.Translate,
                    title = "Your vocabulary is empty",
                    text = "Tap + to add your first word, with its meaning, furigana, notes and example sentences.",
                    modifier = modifier,
                )
            },
        ) { entry ->
            VocabularyCard(entry, onClick = { onOpenEntry(entry.id) })
        }
    }
}

@Composable
private fun VocabularyCard(entry: VocabularyEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RubyText(
                    text = entry.term,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                entry.category?.let { category ->
                    AssistChip(onClick = onClick, label = { Text(category.name) })
                }
            }
            if (entry.meaning.isNotBlank()) {
                RubyText(
                    text = entry.meaning,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
            }
            if (!entry.notes.isBlank) {
                RubyText(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            if (entry.examples.isNotEmpty()) {
                Text(
                    text = if (entry.examples.size == 1) "1 example" else "${entry.examples.size} examples",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
