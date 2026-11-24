package de.miaowz.glossa.ui.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.miaowz.glossa.R
import de.miaowz.glossa.ui.common.EmptyLibrary

/** Dismissible banner shown above cached content when a sync failed. */
@Composable
fun SyncErrorCard(error: SyncError, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Outlined.CloudOff,
                contentDescription = null,
                modifier = Modifier.padding(top = 12.dp),
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = 12.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(R.string.community_error_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(error.message), style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.community_dismiss))
            }
        }
    }
}

/**
 * Full-screen empty or error message ([EmptyLibrary] style) inside a scrollable list, so
 * pull-to-refresh still works when there is nothing else to scroll.
 */
@Composable
fun CommunityMessage(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize()) {
        item {
            EmptyLibrary(icon = icon, title = title, text = text, modifier = Modifier.fillParentMaxSize())
        }
    }
}

/** [CommunityMessage] for a failed first sync, when nothing is cached yet. */
@Composable
fun CommunityErrorMessage(title: String, error: SyncError, modifier: Modifier = Modifier) {
    CommunityMessage(Icons.Outlined.CloudOff, title, stringResource(error.message), modifier)
}
