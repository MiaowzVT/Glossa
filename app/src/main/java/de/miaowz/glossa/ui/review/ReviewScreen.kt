package de.miaowz.glossa.ui.review

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
                    Text(if (reviewing != null) "Repetition · ${reviewing.remaining} left" else "Repetition")
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "End session")
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
            if (state.isFlipped) "Did you know it?" else "Tap the card to reveal the meaning",
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
                Text("Wrong", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = { onAnswer(true) },
                modifier = Modifier.weight(1f).height(64.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(28.dp))
                Text("Correct", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
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
                listOfNotNull(entry.language.name, entry.category?.name, "second try".takeIf { isRetry })
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
    val feedback = feedbackFor(summary.percentCorrect)
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
            Text("${summary.percentCorrect} %", style = MaterialTheme.typography.headlineSmall)
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryRow("Time spent", formatDuration(summary.durationMillis))
                SummaryRow("Correct", summary.correct.toString())
                SummaryRow("Incorrect", summary.incorrect.toString())
                SummaryRow("Correct answers", "${summary.percentCorrect} %")
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
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
        Text("All caught up", style = MaterialTheme.typography.titleMedium)
        Text(
            "No vocabulary is due for repetition right now. Come back later!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onClose) { Text("Back") }
    }
}

private class Feedback(val icon: ImageVector, val title: String, val message: String)

private fun feedbackFor(percentCorrect: Int): Feedback = when {
    percentCorrect == 100 -> Feedback(
        Icons.Outlined.EmojiEvents,
        "Perfect!",
        "Every single answer was right. Outstanding work!",
    )
    percentCorrect >= 85 -> Feedback(
        Icons.Outlined.EmojiEvents,
        "Excellent work!",
        "Your memory is in great shape. Keep it up!",
    )
    percentCorrect >= 65 -> Feedback(
        Icons.Outlined.ThumbUp,
        "Well done!",
        "Solid progress. Every session makes these words stick a little better.",
    )
    percentCorrect >= 40 -> Feedback(
        Icons.Outlined.ThumbUp,
        "Keep going!",
        "Learning takes time. The words you missed will come back soon, and you'll know them better.",
    )
    else -> Feedback(
        Icons.Outlined.Favorite,
        "Don't give up!",
        "Mistakes are how your brain learns. Every session counts, and you'll get there.",
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
