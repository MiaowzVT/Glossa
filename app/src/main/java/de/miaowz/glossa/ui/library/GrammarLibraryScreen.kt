package de.miaowz.glossa.ui.library

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.miaowz.glossa.data.model.GrammarEntry
import de.miaowz.glossa.ruby.RubyText
import de.miaowz.glossa.ui.grammarRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarLibraryScreen(
    onAddEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: GrammarLibraryViewModel = viewModel { GrammarLibraryViewModel(grammarRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(title = { Text("Grammar Library") }, scrollBehavior = scrollBehavior)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Icon(Icons.Filled.Add, contentDescription = "Add grammar entry")
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.totalEntries == 0 -> EmptyLibrary(Modifier.padding(padding))
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "filters") {
                    Filters(
                        state = state,
                        onQueryChange = viewModel::setQuery,
                        onSelectLanguage = viewModel::selectLanguage,
                        onSelectCategory = viewModel::selectCategory,
                    )
                }
                if (state.sections.isEmpty()) {
                    item(key = "no-results") {
                        Text(
                            "No entries match your filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                state.sections.forEach { section ->
                    item(key = "language-${section.language.id}") {
                        Text(
                            text = section.language.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                        )
                    }
                    items(section.entries, key = { it.id }) { entry ->
                        EntryCard(entry, onClick = { onOpenEntry(entry.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Filters(
    state: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onSelectLanguage: (Long?) -> Unit,
    onSelectCategory: (Long?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = state.filter.query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search grammar") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.filter.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = state.filter.languageId == null,
                    onClick = { onSelectLanguage(null) },
                    label = { Text("All languages") },
                )
            }
            items(state.languages, key = { it.id }) { language ->
                FilterChip(
                    selected = state.filter.languageId == language.id,
                    onClick = { onSelectLanguage(language.id) },
                    label = { Text(language.name) },
                )
            }
        }
        if (state.categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.filter.categoryId == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text("All categories") },
                    )
                }
                items(state.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = state.filter.categoryId == category.id,
                        onClick = { onSelectCategory(category.id) },
                        label = { Text(category.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryCard(entry: GrammarEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RubyText(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                entry.category?.let { category ->
                    AssistChip(onClick = onClick, label = { Text(category.name) })
                }
            }
            if (!entry.description.isBlank) {
                RubyText(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
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

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text("Your grammar library is empty", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap + to write your first grammar entry, with formatting, furigana and example sentences.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
