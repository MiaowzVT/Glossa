package de.miaowz.glossa.ruby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RubyMarkupTest {

    @Test
    fun findsAllMarkupsWithPositions() {
        val text = "{今日|きょう}は{雨|あめ}です"
        val matches = RubyMarkup.findAll(text)

        assertEquals(2, matches.size)
        assertEquals("今日", matches[0].base)
        assertEquals("きょう", matches[0].reading)
        assertEquals(0..7, matches[0].range)
        assertEquals("今日", text.substring(matches[0].baseStart, matches[0].baseEnd))
        assertEquals("雨", text.substring(matches[1].baseStart, matches[1].baseEnd))
    }

    @Test
    fun ignoresMalformedMarkup() {
        assertTrue(RubyMarkup.findAll("{漢字}").isEmpty())
        assertTrue(RubyMarkup.findAll("{漢字|}").isEmpty())
        assertTrue(RubyMarkup.findAll("{|かんじ}").isEmpty())
        assertTrue(RubyMarkup.findAll("{ |かんじ}").isEmpty())
        assertTrue(RubyMarkup.findAll("{漢|字|かんじ}").isEmpty())
    }

    @Test
    fun findsInnermostMarkupWhenBracesAreNested() {
        val matches = RubyMarkup.findAll("{a{漢字|かんじ}")
        assertEquals(listOf("漢字"), matches.map { it.base })
    }

    @Test
    fun stripToBaseKeepsOnlyBaseText() {
        assertEquals("今日は雨です", RubyMarkup.stripToBase("{今日|きょう}は{雨|あめ}です"))
        assertEquals("{漢字}", RubyMarkup.stripToBase("{漢字}"))
    }

    @Test
    fun formatAndValidation() {
        assertEquals("{漢字|かんじ}", RubyMarkup.format("漢字", "かんじ"))
        assertTrue(RubyMarkup.isValidPart("かんじ"))
        assertFalse(RubyMarkup.isValidPart(""))
        assertFalse(RubyMarkup.isValidPart("か|ん"))
        assertFalse(RubyMarkup.isValidPart("{か"))
    }
}
