package de.miaowz.glossa.ui.review

import kotlin.math.roundToInt

/** [isRetry]: the card was answered wrong earlier in this session and is asked a second time. */
data class ReviewCard<T>(val item: T, val isRetry: Boolean = false)

/**
 * Queue of one repetition session. A card answered wrong is appended to the end of the queue once;
 * a wrong answer on that second attempt doesn't re-queue it again.
 */
data class ReviewSession<T>(
    val queue: List<ReviewCard<T>>,
    val correct: Int = 0,
    val incorrect: Int = 0,
) {
    val current: ReviewCard<T>? get() = queue.firstOrNull()
    val isFinished: Boolean get() = queue.isEmpty()
    val answered: Int get() = correct + incorrect

    fun answer(isCorrect: Boolean): ReviewSession<T> {
        val card = queue.firstOrNull() ?: return this
        val retry = if (!isCorrect && !card.isRetry) listOf(card.copy(isRetry = true)) else emptyList()
        return copy(
            queue = queue.drop(1) + retry,
            correct = correct + if (isCorrect) 1 else 0,
            incorrect = incorrect + if (isCorrect) 0 else 1,
        )
    }

    companion object {
        fun <T> of(items: List<T>) = ReviewSession(items.map { ReviewCard(it) })
    }
}

/** Counts are answers, so a card answered wrong and then right counts once on each side. */
data class ReviewSummary(
    val durationMillis: Long,
    val correct: Int,
    val incorrect: Int,
) {
    val percentCorrect: Int
        get() = if (correct + incorrect == 0) 0 else (correct * 100f / (correct + incorrect)).roundToInt()
}
