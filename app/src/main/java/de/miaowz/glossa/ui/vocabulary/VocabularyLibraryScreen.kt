package de.miaowz.glossa.ui.vocabulary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.miaowz.glossa.data.model.VocabularyEntry
import de.miaowz.glossa.ruby.RubyText
import de.miaowz.glossa.ui.common.EmptyLibrary
import de.miaowz.glossa.ui.common.LibraryList
import de.miaowz.glossa.ui.vocabularyRepository
import de.miaowz.glossa.ui.theme.GlossaTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyLibraryScreen(
    onAddEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: VocabularyLibraryViewModel = viewModel {
        VocabularyLibraryViewModel(vocabularyRepository)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Vocabulary") },
                scrollBehavior = scrollBehavior,
            )
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
            VocabularyCard(
                entry = entry,
                onClick = { onOpenEntry(entry.id) },
            )
        }
    }
}

@Composable
private fun VocabularyCard(entry: VocabularyEntry, onClick: () -> Unit) {
    // Keep RubyText: vocabulary entries may contain furigana and formatted text.
    VocabularyCardContent(
        categoryName = entry.category?.name,
        exampleCount = entry.examples.size,
        onClick = onClick,
        term = {
            RubyText(
                text = entry.term,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
        },
        meaning = {
            if (entry.meaning.isNotBlank()) {
                RubyText(
                    text = entry.meaning,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
            }
        },
        notes = {
            if (!entry.notes.isBlank) {
                RubyText(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        },
    )
}
/** Shared card layout so the preview shows the same surface as real vocabulary entries. */
@Composable
private fun VocabularyCardContent(
    categoryName: String?,
    exampleCount: Int,
    onClick: () -> Unit,
    term: @Composable () -> Unit,
    meaning: @Composable () -> Unit,
    notes: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
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
                imageVector = Icons.Outlined.Translate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

            Box(modifier = Modifier.weight(1f)) {
                term()
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        meaning()
        notes()
        if (categoryName != null || exampleCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                categoryName?.let { category ->
                    AssistChip(
                        onClick = onClick,
                        label = { Text(category) },
                    )
                }
                if (exampleCount > 0) {
                    Text(
                        text = if (exampleCount == 1) "1 example" else "$exampleCount examples",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
}
@Preview(showBackground = true, name = "Vocabulary card")
@Composable
private fun VocabularyCardPreview() {
    GlossaTheme(dynamicColor = false) {
        VocabularyCardContent(
            categoryName = "Greetings",
            exampleCount = 2,
            onClick = {},
            term = {
                Text(
                    text = "こんにちは",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            meaning = {
                Text(
                    text = "Hello",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            notes = {
                Text(
                    text = "A common Japanese greeting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}

