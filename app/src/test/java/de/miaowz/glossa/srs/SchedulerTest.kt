package de.miaowz.glossa.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class SchedulerTest {

    private val fsrs = Fsrs()
    private val zone = ZoneId.of("Europe/Berlin")

    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun newCardAnsweredGoodIsDueAtStartOfDayAfterInterval() {
        val now = at("2026-11-17T21:30")
        val state = fsrs.schedule(null, Rating.Good, now, zone)
        // Initial stability for Good is 3.173 days → 3 days.
        assertEquals(at("2026-11-20T00:00"), state.due)
        assertEquals(now, state.lastReview)
        assertEquals(1, state.reps)
        assertEquals(0, state.lapses)
    }

    @Test
    fun failedCardIsDueTomorrowAndCountsLapse() {
        val first = fsrs.schedule(null, Rating.Good, at("2026-11-17T10:00"), zone)
        val failed = fsrs.schedule(first, Rating.Again, at("2026-11-20T09:00"), zone)
        assertEquals(at("2026-11-21T00:00"), failed.due)
        assertEquals(1, failed.lapses)
        assertEquals(2, failed.reps)
        assertTrue(failed.stability < first.stability)
    }

    @Test
    fun retryInSameSessionCountsAsSameDayReview() {
        val failed = fsrs.schedule(null, Rating.Again, at("2026-11-17T23:00"), zone)
        val retried = fsrs.schedule(failed, Rating.Good, at("2026-11-17T23:05"), zone)
        val expected = fsrs.nextState(MemoryState(failed.stability, failed.difficulty), 0, Rating.Good)
        assertEquals(expected.stability, retried.stability, 1e-9)
        // A first answer of Again on a new card is not a lapse.
        assertEquals(0, retried.lapses)
    }

    @Test
    fun elapsedDaysAreCalendarDays() {
        // 23:50 → 00:10 is one calendar day, even though only 20 minutes passed.
        val first = fsrs.schedule(null, Rating.Good, at("2026-11-17T23:50"), zone)
        val next = fsrs.schedule(first, Rating.Good, at("2026-11-18T00:10"), zone)
        val expected = fsrs.nextState(MemoryState(first.stability, first.difficulty), 1, Rating.Good)
        assertEquals(expected.stability, next.stability, 1e-9)
    }
}
