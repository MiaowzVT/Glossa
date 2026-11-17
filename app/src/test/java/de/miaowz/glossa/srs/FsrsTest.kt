package de.miaowz.glossa.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsTest {

    private val fsrs = Fsrs()

    @Test
    fun initialStateUsesDefaultParameters() {
        assertEquals(MemoryState(0.40255, 7.1949), fsrs.initialState(Rating.Again))
        val good = fsrs.initialState(Rating.Good)
        assertEquals(3.173, good.stability, 1e-9)
        // w4 - e^(2 * w5) + 1
        assertEquals(5.2825, good.difficulty, 1e-3)
    }

    @Test
    fun retrievabilityIsNinetyPercentAfterStabilityDays() {
        assertEquals(0.9, fsrs.retrievability(10.0, 10.0), 1e-3)
        assertEquals(1.0, fsrs.retrievability(0.0, 10.0), 1e-9)
    }

    @Test
    fun intervalEqualsStabilityAtNinetyPercentRetention() {
        assertEquals(10, fsrs.nextInterval(10.0))
        assertEquals(1, fsrs.nextInterval(0.1))
        assertEquals(36_500, fsrs.nextInterval(1e9))
        assertTrue(Fsrs(desiredRetention = 0.8).nextInterval(10.0) > 10)
    }

    @Test
    fun goodIncreasesAndAgainDecreasesStability() {
        val state = MemoryState(stability = 10.0, difficulty = 5.0)
        val good = fsrs.nextState(state, elapsedDays = 10, Rating.Good)
        val again = fsrs.nextState(state, elapsedDays = 10, Rating.Again)
        assertTrue(good.stability > state.stability)
        assertTrue(again.stability < state.stability)
        assertTrue(again.difficulty > state.difficulty)
        assertTrue(good.difficulty <= state.difficulty)
    }

    @Test
    fun laterSuccessfulReviewsGrowStabilityMore() {
        val state = MemoryState(stability = 10.0, difficulty = 5.0)
        val early = fsrs.nextState(state, elapsedDays = 2, Rating.Good)
        val onTime = fsrs.nextState(state, elapsedDays = 10, Rating.Good)
        assertTrue(onTime.stability > early.stability)
    }

    @Test
    fun sameDayReviewUsesShortTermStability() {
        val state = fsrs.initialState(Rating.Again)
        val good = fsrs.nextState(state, elapsedDays = 0, Rating.Good)
        val again = fsrs.nextState(state, elapsedDays = 0, Rating.Again)
        assertTrue(good.stability >= state.stability)
        assertTrue(again.stability < state.stability)
    }

    @Test
    fun difficultyStaysInRange() {
        var state = fsrs.initialState(Rating.Again)
        repeat(50) { state = fsrs.nextState(state, elapsedDays = 1, Rating.Again) }
        assertTrue(state.difficulty in 1.0..10.0)
        repeat(50) { state = fsrs.nextState(state, elapsedDays = 1, Rating.Easy) }
        assertTrue(state.difficulty in 1.0..10.0)
    }
}
