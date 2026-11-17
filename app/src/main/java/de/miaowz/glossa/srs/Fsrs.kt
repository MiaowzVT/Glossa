package de.miaowz.glossa.srs

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Answer grade. The review screen only uses [Again] (✗) and [Good] (✓). */
enum class Rating(val grade: Int) { Again(1), Hard(2), Good(3), Easy(4) }

/**
 * [stability]: days until the recall probability drops to 90 %.
 * [difficulty]: 1 (easy) to 10 (hard).
 */
data class MemoryState(val stability: Double, val difficulty: Double)

/**
 * FSRS-5 (Free Spaced Repetition Scheduler) with its default parameters.
 * See https://github.com/open-spaced-repetition/fsrs4anki/wiki/The-Algorithm
 */
class Fsrs(
    private val w: DoubleArray = DEFAULT_PARAMETERS,
    val desiredRetention: Double = 0.9,
    val maximumInterval: Int = 36_500,
) {
    init {
        require(w.size == DEFAULT_PARAMETERS.size) { "FSRS-5 needs ${DEFAULT_PARAMETERS.size} parameters" }
    }

    /** Probability of recalling a card with [stability] after [elapsedDays]. */
    fun retrievability(elapsedDays: Double, stability: Double): Double =
        (1 + FACTOR * elapsedDays / stability).pow(DECAY)

    fun initialState(rating: Rating): MemoryState =
        MemoryState(max(w[rating.grade - 1], MIN_STABILITY), initialDifficulty(rating).coerceIn(1.0, 10.0))

    /** Memory state after a review [elapsedDays] (calendar days) after the previous one. */
    fun nextState(state: MemoryState, elapsedDays: Int, rating: Rating): MemoryState {
        val (s, d) = state
        val stability = if (elapsedDays <= 0) {
            shortTermStability(s, rating)
        } else {
            val r = retrievability(elapsedDays.toDouble(), s)
            if (rating == Rating.Again) forgetStability(d, s, r) else recallStability(d, s, r, rating)
        }
        return MemoryState(max(stability, MIN_STABILITY), nextDifficulty(d, rating))
    }

    /** Days until the recall probability drops to [desiredRetention]. */
    fun nextInterval(stability: Double): Int {
        val days = stability / FACTOR * (desiredRetention.pow(1 / DECAY) - 1)
        return days.roundToInt().coerceIn(1, maximumInterval)
    }

    private fun initialDifficulty(rating: Rating): Double = w[4] - exp(w[5] * (rating.grade - 1)) + 1

    private fun nextDifficulty(d: Double, rating: Rating): Double {
        val delta = -w[6] * (rating.grade - 3)
        val damped = d + delta * (10 - d) / 9
        // Mean reversion towards the initial difficulty of an "Easy" card.
        return (w[7] * initialDifficulty(Rating.Easy) + (1 - w[7]) * damped).coerceIn(1.0, 10.0)
    }

    private fun recallStability(d: Double, s: Double, r: Double, rating: Rating): Double {
        val hardPenalty = if (rating == Rating.Hard) w[15] else 1.0
        val easyBonus = if (rating == Rating.Easy) w[16] else 1.0
        return s * (1 + exp(w[8]) * (11 - d) * s.pow(-w[9]) * (exp((1 - r) * w[10]) - 1) * hardPenalty * easyBonus)
    }

    private fun forgetStability(d: Double, s: Double, r: Double): Double {
        val forget = w[11] * d.pow(-w[12]) * ((s + 1).pow(w[13]) - 1) * exp((1 - r) * w[14])
        return min(forget, s / exp(w[17] * w[18]))
    }

    /** Reviews on the same day as the previous one. */
    private fun shortTermStability(s: Double, rating: Rating): Double {
        val increase = exp(w[17] * (rating.grade - 3 + w[18]))
        return s * if (rating >= Rating.Good) max(increase, 1.0) else increase
    }

    companion object {
        private const val DECAY = -0.5
        private const val FACTOR = 19.0 / 81.0
        private const val MIN_STABILITY = 0.01

        val DEFAULT_PARAMETERS = doubleArrayOf(
            0.40255, 1.18385, 3.173, 15.69105, 7.1949, 0.5345, 1.4604, 0.0046, 1.54575, 0.1192,
            1.01925, 1.9395, 0.11, 0.29605, 2.2698, 0.2315, 2.9898, 0.51655, 0.6621,
        )
    }
}
