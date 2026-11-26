package de.miaowz.glossa.ui.grammar

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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.miaowz.glossa.R
import de.miaowz.glossa.data.model.GrammarEntry
import de.miaowz.glossa.ruby.RubyText
import de.miaowz.glossa.ui.common.EmptyLibrary
import de.miaowz.glossa.ui.common.LibraryList
import de.miaowz.glossa.ui.grammarRepository
import de.miaowz.glossa.ui.theme.GlossaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarLibraryScreen(
    onAddEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: GrammarLibraryViewModel = viewModel {
        GrammarLibraryViewModel(grammarRepository)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.grammar_library)) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_grammar_entry),
                )
            }
        },
    ) { padding ->
        LibraryList(
            state = state,
            padding = padding,
            searchPlaceholder = stringResource(R.string.search_grammar),
            onQueryChange = viewModel::setQuery,
            onSelectLanguage = viewModel::selectLanguage,
            onSelectCategory = viewModel::selectCategory,
            key = { it.id },
            empty = { modifier ->
                EmptyLibrary(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = stringResource(R.string.grammar_library_empty),
                    text = stringResource(R.string.grammar_library_empty_detail),
                    modifier = modifier,
                )
            },
        ) { entry ->
            EntryCard(
                entry = entry,
                onClick = { onOpenEntry(entry.id) },
            )
        }
    }
}

@Composable
private fun EntryCard(entry: GrammarEntry, onClick: () -> Unit) {
    // Preserve RubyText for formatted grammar content and Japanese ruby markup.
    GrammarCardContent(
        categoryName = entry.category?.name,
        exampleText = if (entry.examples.isNotEmpty()) {
            pluralStringResource(
                R.plurals.grammar_examples,
                entry.examples.size,
                entry.examples.size,
            )
        } else {
            null
        },
        onClick = onClick,
        title = {
            RubyText(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
        },
        description = {
            if (!entry.description.isBlank) {
                RubyText(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                )
            }
        },
    )
}

/** Matches the modern vocabulary card layout without changing grammar data handling. */
@Composable
private fun GrammarCardContent(
    categoryName: String?,
    exampleText: String?,
    onClick: () -> Unit,
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
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
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    title()
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            description()

            if (categoryName != null || exampleText != null) {
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
                    if (exampleText != null) {
                        Text(
                            text = exampleText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Grammar card")
@Composable
private fun GrammarCardPreview() {
    GlossaTheme(dynamicColor = false) {
        GrammarCardContent(
            categoryName = "Particles",
            exampleText = "2 examples",
            onClick = {},
            title = {
                Text(
                    text = "は (wa)",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            description = {
                Text(
                    text = "Marks the topic of a sentence.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}
