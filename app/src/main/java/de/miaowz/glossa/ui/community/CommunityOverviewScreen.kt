package de.miaowz.glossa.ui.community

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.miaowz.glossa.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityOverviewScreen(onButtonClick: (CommunityScreenTarget) -> Unit) {
    RequestLocalNetworkPermission()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.community_overview_title)) }, scrollBehavior = scrollBehavior) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { OfficialSetsCard(onClick = { onButtonClick(CommunityScreenTarget.OFFICIAL_SETS_MAIN_PAGE) }) }
            item { CommunitySetsCard(onClick = { onButtonClick(CommunityScreenTarget.COMMUNITY_SETS_MAIN_PAGE) }) }
        }
    }
}

/**
 * Android 17 blocks connections to private/LAN addresses (the dev backend at 10.0.2.2 or
 * 192.168.x.x) unless the app holds ACCESS_LOCAL_NETWORK; without it, connects silently time out.
 */
@Composable
private fun RequestLocalNetworkPermission() {
    if (Build.VERSION.SDK_INT < 37) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(ACCESS_LOCAL_NETWORK)
    }
}

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

@Composable
private fun OfficialSetsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        onClick = onClick
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.community_official_sets_card_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.community_official_sets_card_text),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunitySetsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        onClick = onClick
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.community_sets_card_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.community_sets_card_text),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}