package com.pressione.iperteso.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeBandTest {

    @Test
    fun `default bands are correct`() {
        val bands = TimeBand.defaults()
        assertEquals(4, bands.size)
        assertEquals("MORNING", bands[0].key)
        assertEquals("AFTERNOON", bands[1].key)
        assertEquals("EVENING", bands[2].key)
        assertEquals("NIGHT", bands[3].key)
    }

    @Test
    fun `morning band contains correct hours`() {
        val morning = TimeBand("MORNING", "Mattina", 6, 12)
        assertTrue(morning.contains(6))
        assertTrue(morning.contains(9))
        assertTrue(morning.contains(11))
        assertFalse(morning.contains(5))
        assertFalse(morning.contains(12))
        assertFalse(morning.contains(13))
    }

    @Test
    fun `afternoon band contains correct hours`() {
        val afternoon = TimeBand("AFTERNOON", "Pomeriggio", 12, 18)
        assertTrue(afternoon.contains(12))
        assertTrue(afternoon.contains(15))
        assertTrue(afternoon.contains(17))
        assertFalse(afternoon.contains(11))
        assertFalse(afternoon.contains(18))
    }

    @Test
    fun `evening band contains correct hours`() {
        val evening = TimeBand("EVENING", "Sera", 18, 22)
        assertTrue(evening.contains(18))
        assertTrue(evening.contains(20))
        assertTrue(evening.contains(21))
        assertFalse(evening.contains(17))
        assertFalse(evening.contains(22))
    }

    @Test
    fun `overnight band wraps around midnight`() {
        val night = TimeBand("NIGHT", "Notte", 22, 6)
        assertTrue(night.contains(22))
        assertTrue(night.contains(23))
        assertTrue(night.contains(0))
        assertTrue(night.contains(3))
        assertTrue(night.contains(5))
        assertFalse(night.contains(6))
        assertFalse(night.contains(12))
        assertFalse(night.contains(21))
    }

    @Test
    fun `band with same start and end is empty`() {
        val band = TimeBand("ZERO", "Zero", 10, 10)
        assertFalse(band.contains(10))
        assertFalse(band.contains(9))
        assertFalse(band.contains(11))
    }
}
