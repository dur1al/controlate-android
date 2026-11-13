package com.dur1al.controlate

import com.dur1al.controlate.porteria.utils.RutUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RutUtilsTest {

    @Test
    fun `formatRut formats standard and K check-digit values`() {
        assertEquals("11.111.111-1", RutUtils.formatRut("111111111"))
        assertEquals("12.345.678-5", RutUtils.formatRut("12.345.678-5"))
        assertEquals("7.654.321-K", RutUtils.formatRut("7654321k"))
    }

    @Test
    fun `isValidRut validates authentic check digits and rejects invalid ones`() {
        // 11.111.111-1 y 12.345.678-5 son RUTs matemáticos válidos de prueba
        assertTrue(RutUtils.isValidRut("11.111.111-1"))
        assertTrue(RutUtils.isValidRut("12345678-5"))
        assertFalse(RutUtils.isValidRut("11.111.111-2"))
        assertFalse(RutUtils.isValidRut("1"))
        assertFalse(RutUtils.isValidRut(""))
    }

    @Test
    fun `cleanRut strips punctuation and uppercases k`() {
        assertEquals("7654321K", RutUtils.cleanRut("7.654.321-k"))
    }
}
