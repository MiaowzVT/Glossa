package de.miaowz.glossa.srs

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Scheduling state of one card. Timestamps are epoch milliseconds. */
data class CardState(
    val stability: Double,
    val difficulty: Double,
    val due: Long,
    val lastReview: Long,
    val reps: Int,
    val lapses: Int,
)

/**
 * Reviews a card ([previous] is null for a new card) with [rating] at [now]. Days are calendar days
 * in [zone]: the card becomes due at the start of the day its interval ends on.
 */
fun Fsrs.schedule(previous: CardState?, rating: Rating, now: Long, zone: ZoneId): CardState {
    val today = localDate(now, zone)
    val memory = if (previous == null) {
        initialState(rating)
    } else {
        val elapsedDays = ChronoUnit.DAYS.between(localDate(previous.lastReview, zone), today)
        nextState(MemoryState(previous.stability, previous.difficulty), elapsedDays.toInt(), rating)
    }
    val due = today.plusDays(nextInterval(memory.stability).toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
    return CardState(
        stability = memory.stability,
        difficulty = memory.difficulty,
        due = due,
        lastReview = now,
        reps = (previous?.reps ?: 0) + 1,
        lapses = (previous?.lapses ?: 0) + if (previous != null && rating == Rating.Again) 1 else 0,
    )
}

private fun localDate(epochMillis: Long, zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
