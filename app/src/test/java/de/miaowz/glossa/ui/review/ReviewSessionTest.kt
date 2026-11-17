package de.miaowz.glossa.ui.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSessionTest {

    @Test
    fun correctAnswersGoThroughTheQueueOnce() {
        val session = ReviewSession.of(listOf("a", "b")).answer(true).answer(true)
        assertTrue(session.isFinished)
        assertNull(session.current)
        assertEquals(2, session.correct)
        assertEquals(0, session.incorrect)
    }

    @Test
    fun wrongCardIsAskedOnceMoreAtTheEnd() {
        var session = ReviewSession.of(listOf("a", "b"))
        session = session.answer(false)
        assertEquals(listOf(ReviewCard("b"), ReviewCard("a", isRetry = true)), session.queue)

        session = session.answer(true).answer(true)
        assertTrue(session.isFinished)
        assertEquals(2, session.correct)
        assertEquals(1, session.incorrect)
    }

    @Test
    fun wrongCardIsNotAskedAThirdTime() {
        val session = ReviewSession.of(listOf("a")).answer(false).answer(false)
        assertTrue(session.isFinished)
        assertEquals(0, session.correct)
        assertEquals(2, session.incorrect)
        assertEquals(2, session.answered)
    }

    @Test
    fun answeringFinishedSessionDoesNothing() {
        val session = ReviewSession.of(emptyList<String>())
        assertEquals(session, session.answer(true))
    }

    @Test
    fun percentCorrectIsRounded() {
        assertEquals(67, ReviewSummary(0, correct = 2, incorrect = 1).percentCorrect)
        assertEquals(100, ReviewSummary(0, correct = 3, incorrect = 0).percentCorrect)
        assertEquals(0, ReviewSummary(0, correct = 0, incorrect = 0).percentCorrect)
    }
}
