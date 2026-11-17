package de.miaowz.glossa.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Body of a library screen: loading indicator, [empty] content when there are no entries at all,
 * otherwise search + filter chips and one section per language.
 */
@Composable
fun <T> LibraryList(
    state: LibraryUiState<T>,
    padding: PaddingValues,
    searchPlaceholder: String,
    onQueryChange: (String) -> Unit,
    onSelectLanguage: (Long?) -> Unit,
    onSelectCategory: (Long?) -> Unit,
    key: (T) -> Any,
    empty: @Composable (Modifier) -> Unit,
    item: @Composable (T) -> Unit,
) {
    when {
        state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
            CircularProgressIndicator()
        }
        state.totalEntries == 0 -> empty(Modifier.padding(padding))
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
                    searchPlaceholder = searchPlaceholder,
                    onQueryChange = onQueryChange,
                    onSelectLanguage = onSelectLanguage,
                    onSelectCategory = onSelectCategory,
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
                items(section.entries, key = key) { entry -> item(entry) }
            }
        }
    }
}

@Composable
private fun Filters(
    state: LibraryUiState<*>,
    searchPlaceholder: String,
    onQueryChange: (String) -> Unit,
    onSelectLanguage: (Long?) -> Unit,
    onSelectCategory: (Long?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = state.filter.query,
            onValueChange = onQueryChange,
            placeholder = { Text(searchPlaceholder) },
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
fun EmptyLibrary(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
