package de.miaowz.glossa.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.miaowz.glossa.data.VocabularyRepository
import de.miaowz.glossa.data.model.VocabularyEntry
import de.miaowz.glossa.srs.Rating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

sealed interface ReviewUiState {
    data object Loading : ReviewUiState
    data object NothingDue : ReviewUiState
    data class Reviewing(
        val card: ReviewCard<VocabularyEntry>,
        val isFlipped: Boolean,
        /** Cards still in the queue, including the current one. */
        val remaining: Int,
        val answered: Int,
    ) : ReviewUiState
    data class Finished(val summary: ReviewSummary) : ReviewUiState
}

class ReviewViewModel(private val repository: VocabularyRepository) : ViewModel() {

    private var session = ReviewSession<VocabularyEntry>(emptyList())
    private var startedAt = TimeSource.Monotonic.markNow()
    private var isFlipped = false
    // Reviews of the same card must be written in answer order.
    private val writes = Mutex()

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            session = ReviewSession.of(repository.getDueEntries())
            startedAt = TimeSource.Monotonic.markNow()
            _uiState.value = if (session.isFinished) ReviewUiState.NothingDue else reviewingState()
        }
    }

    fun flip() {
        if (_uiState.value !is ReviewUiState.Reviewing) return
        isFlipped = !isFlipped
        _uiState.value = reviewingState()
    }

    fun answer(isCorrect: Boolean) {
        val card = session.current ?: return
        if (_uiState.value !is ReviewUiState.Reviewing) return
        viewModelScope.launch {
            writes.withLock { repository.review(card.item.id, if (isCorrect) Rating.Good else Rating.Again) }
        }
        session = session.answer(isCorrect)
        isFlipped = false
        _uiState.value = if (session.isFinished) {
            ReviewUiState.Finished(
                ReviewSummary(startedAt.elapsedNow().inWholeMilliseconds, session.correct, session.incorrect),
            )
        } else {
            reviewingState()
        }
    }

    private fun reviewingState() = ReviewUiState.Reviewing(
        card = session.current!!,
        isFlipped = isFlipped,
        remaining = session.queue.size,
        answered = session.answered,
    )
}
