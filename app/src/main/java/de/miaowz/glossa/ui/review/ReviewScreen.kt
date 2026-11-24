package de.miaowz.glossa.ui.review

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.miaowz.glossa.R
import de.miaowz.glossa.data.model.VocabularyEntry
import de.miaowz.glossa.ruby.RubyMarkup
import de.miaowz.glossa.ruby.RubyText
import de.miaowz.glossa.ui.vocabularyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onClose: () -> Unit,
    viewModel: ReviewViewModel = viewModel { ReviewViewModel(vocabularyRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val reviewing = state as? ReviewUiState.Reviewing
                    Text(if (reviewing != null) stringResource(R.string.cards_left, reviewing.remaining) else stringResource(
                        R.string.repitition))
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.end_session))
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (val current = state) {
            ReviewUiState.Loading -> Box(modifier, Alignment.Center) { CircularProgressIndicator() }
            ReviewUiState.NothingDue -> NothingDue(onClose, modifier)
            is ReviewUiState.Reviewing -> Reviewing(
                state = current,
                onFlip = viewModel::flip,
                onAnswer = viewModel::answer,
                modifier = modifier,
            )
            is ReviewUiState.Finished -> Summary(current.summary, onClose, modifier)
        }
    }
}

@Composable
private fun Reviewing(
    state: ReviewUiState.Reviewing,
    onFlip: () -> Unit,
    onAnswer: (isCorrect: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LinearProgressIndicator(
            progress = { state.answered.toFloat() / (state.answered + state.remaining) },
            modifier = Modifier.fillMaxWidth(),
        )
        // New composition per card, so the flip animation never shows the next card's back side.
        key(state.card.item.id, state.card.isRetry) {
            Flashcard(
                entry = state.card.item,
                isFlipped = state.isFlipped,
                isRetry = state.card.isRetry,
                onFlip = onFlip,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
        Text(
            if (state.isFlipped) stringResource(R.string.did_you_know_it) else stringResource(R.string.tap_to_reveal),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { onAnswer(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.weight(1f).height(64.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(stringResource(R.string.wrong), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = { onAnswer(true) },
                modifier = Modifier.weight(1f).height(64.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(stringResource(R.string.correct), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Front: the term without readings. Back: term with furigana, meaning and notes. */
@Composable
private fun Flashcard(
    entry: VocabularyEntry,
    isFlipped: Boolean,
    isRetry: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(if (isFlipped) 180f else 0f, tween(400), label = "flip")
    ElevatedCard(
        onClick = onFlip,
        modifier = modifier.graphicsLayer {
            rotationY = rotation
            cameraDistance = 12f * density
        },
    ) {
        Box(Modifier.fillMaxSize().padding(24.dp)) {
            Text(
                listOfNotNull(entry.language.name, entry.category?.name, stringResource(R.string.second_try).takeIf { isRetry })
                    .joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { if (rotation > 90f) rotationY = 180f },
            )
            if (rotation <= 90f) {
                RubyText(
                    text = RubyMarkup.stripToBase(entry.term),
                    style = MaterialTheme.typography.displaySmall.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer { rotationY = 180f }
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RubyText(
                        text = entry.term,
                        style = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 32.dp))
                    if (entry.meaning.isNotBlank()) {
                        RubyText(
                            text = entry.meaning,
                            style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                        )
                    }
                    if (!entry.notes.isBlank) {
                        RubyText(
                            text = entry.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 6,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(summary: ReviewSummary, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val feedback = feedbackFor(summary.percentCorrect, LocalContext.current)
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            feedback.icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(feedback.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            feedback.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
            CircularProgressIndicator(
                progress = { summary.percentCorrect / 100f },
                modifier = Modifier.size(120.dp),
                strokeWidth = 10.dp,
            )
            Text(stringResource(R.string.summary_percent_correct, summary.percentCorrect), style = MaterialTheme.typography.headlineSmall)
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryRow(stringResource(R.string.time_spent), formatDuration(summary.durationMillis))
                SummaryRow(stringResource(R.string.correct), summary.correct.toString())
                SummaryRow(stringResource(R.string.incorrect), summary.incorrect.toString())
                SummaryRow(stringResource(R.string.correct_answers), stringResource(R.string.summary_percent_correct, summary.percentCorrect))
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.done)) }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun NothingDue(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.TaskAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.all_caught_up), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.all_caught_up_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onClose) { Text(stringResource(R.string.back)) }
    }
}

private class Feedback(val icon: ImageVector, val title: String, val message: String)

private fun feedbackFor(percentCorrect: Int, context: Context): Feedback = when {
    percentCorrect == 100 -> Feedback(
        Icons.Outlined.EmojiEvents,
        context.resources.getString(R.string.perfect),
        context.resources.getString(R.string.all_perfect),
    )
    percentCorrect >= 85 -> Feedback(
        Icons.Outlined.EmojiEvents,
        context.resources.getString(R.string.excellent_work),
        context.resources.getString(R.string.excellent_work_detail),
    )
    percentCorrect >= 65 -> Feedback(
        Icons.Outlined.ThumbUp,
        context.resources.getString(R.string.well_done),
        context.resources.getString(R.string.well_done_detail),
    )
    percentCorrect >= 40 -> Feedback(
        Icons.Outlined.ThumbUp,
        context.resources.getString(R.string.keep_going),
        context.resources.getString(R.string.keep_going_detail),
    )
    else -> Feedback(
        Icons.Outlined.Favorite,
        context.resources.getString(R.string.dont_give_up),
        context.resources.getString(R.string.dont_give_up_detail),
    )
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "$hours h $minutes min"
        minutes > 0 -> "$minutes min $seconds s"
        else -> "$seconds s"
    }
}
