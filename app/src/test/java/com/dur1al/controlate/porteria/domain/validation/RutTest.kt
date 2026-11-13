package com.dur1al.controlate.porteria.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RutTest {

    @Test
    fun `normalize strips punctuation, spaces and uppercases the check digit`() {
        assertEquals("7654321K", Rut.normalize(" 7.654.321-k "))
        assertEquals("123456785", Rut.normalize("12.345.678-5"))
    }

    @Test
    fun `normalize drops a K that is not in check digit position`() {
        // Una K en el cuerpo no es un RUT posible; se descarta para no romper el cálculo del DV.
        assertEquals("12456785", Rut.normalize("12K45678-5"))
    }

    @Test
    fun `isValid accepts correct check digits including K`() {
        assertTrue(Rut.isValid("11.111.111-1"))
        assertTrue(Rut.isValid("12345678-5"))
        assertTrue(Rut.isValid("10.000.013-K"))
    }

    @Test
    fun `isValid rejects wrong check digit, too short or empty input`() {
        assertFalse(Rut.isValid("11.111.111-2"))
        assertFalse(Rut.isValid("1"))
        assertFalse(Rut.isValid(""))
        assertFalse(Rut.isValid("123456"))
    }

    @Test
    fun `isValid never throws for a K inside the body`() {
        assertFalse(Rut.isValid("12K45678-9"))
    }

    @Test
    fun `format renders thousands separators and check digit`() {
        assertEquals("11.111.111-1", Rut.format("111111111"))
        assertEquals("7.654.321-K", Rut.format("7654321k"))
        assertEquals("1", Rut.format("1"))
    }

    @Test
    fun `normalizeOrNull returns null for blank input`() {
        assertNull(Rut.normalizeOrNull("  "))
        assertNull(Rut.normalizeOrNull(null))
        assertEquals("111111111", Rut.normalizeOrNull("11.111.111-1"))
    }
}
