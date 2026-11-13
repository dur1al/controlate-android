package com.dur1al.controlate.porteria.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Matriz QA de [Rut.isValid]: cada formato que un guardia puede escribir o pegar.
 * Todos los RUT son sintéticos (el DV se calcula, no corresponde a personas reales).
 */
class RutQaTest {

    private fun withDv(body: String) = body + Rut.computeCheckDigit(body)

    @Test
    fun `accepts the three supported writing styles`() {
        listOf("12.345.678-5", "12345678-5", "123456785", " 12.345.678-5 ").forEach {
            assertTrue("debe aceptar '$it'", Rut.isValid(it))
        }
    }

    @Test
    fun `accepts K check digit in upper and lower case`() {
        assertTrue(Rut.isValid("10.000.013-K"))
        assertTrue(Rut.isValid("10.000.013-k"))
        assertTrue(Rut.isValid("10000013k"))
    }

    @Test
    fun `accepts bodies of 6, 7 and 8 digits`() {
        listOf("100000", "1234567", "12345678").forEach { body ->
            val rut = withDv(body)
            assertTrue("debe aceptar $rut", Rut.isValid(rut))
            assertTrue("debe aceptar ${Rut.format(rut)}", Rut.isValid(Rut.format(rut)))
        }
    }

    @Test
    fun `rejects a wrong check digit`() {
        assertFalse(Rut.isValid("12.345.678-4"))
        assertFalse(Rut.isValid("10.000.013-0"))
    }

    @Test
    fun `rejects misplaced or partial thousands separators`() {
        listOf("12.345678-5", "1234.5678-5", "12.34.5678-5", ".12.345.678-5", "12.345.678.-5", "12..345.678-5").forEach {
            assertFalse("debe rechazar '$it'", Rut.isValid(it))
        }
    }

    @Test
    fun `rejects letters, K in the body and extra symbols`() {
        listOf("12K45678-9", "A2.345.678-5", "12.345.678-5X", "12-345-678-5", "12.345.678--5", "12 345 678-5", "12.345.678/5").forEach {
            assertFalse("debe rechazar '$it'", Rut.isValid(it))
        }
    }

    @Test
    fun `rejects out of range lengths and leading zero bodies`() {
        assertFalse(Rut.isValid(withDv("12345")))
        assertFalse(Rut.isValid(withDv("123456789")))
        assertFalse(Rut.isValid(withDv("01234567")))
        assertFalse(Rut.isValid("-"))
        assertFalse(Rut.isValid("K"))
        assertFalse(Rut.isValid("   "))
    }

    @Test
    fun `normalize and format round trip keeps the same rut`() {
        val canonical = withDv("12345678")
        assertEquals(canonical, Rut.normalize(Rut.format(canonical)))
        assertEquals("12.345.678-5", Rut.format(Rut.normalize("12345678-5")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `check digit requires a numeric body`() {
        Rut.computeCheckDigit("12K4")
    }
}
