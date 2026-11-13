package com.dur1al.controlate.porteria.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TimeFormatTest {

    private val zone = ZoneId.of("America/Santiago")
    private fun epoch(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `time renders 24h clock in the given zone`() {
        assertEquals("21:05", TimeFormat.time(epoch(2026, 6, 14, 21, 5), zone))
    }

    @Test
    fun `isoDate and displayDate render the local calendar day`() {
        val at = epoch(2026, 6, 14, 23, 59)
        assertEquals("2026-06-14", TimeFormat.isoDate(at, zone))
        assertEquals("14/06/2026", TimeFormat.displayDate(at, zone))
    }

    @Test
    fun `dayRange covers local midnight to next midnight`() {
        val range = TimeFormat.dayRange(LocalDate.of(2026, 6, 14), zone)
        assertEquals(epoch(2026, 6, 14, 0, 0), range.first)
        assertEquals(epoch(2026, 6, 15, 0, 0) - 1, range.last)
    }

    @Test
    fun `isoToDisplay converts and leaves unknown input untouched`() {
        assertEquals("14/06/2026", TimeFormat.isoToDisplay("2026-06-14"))
        assertEquals("ayer", TimeFormat.isoToDisplay("ayer"))
    }
}
