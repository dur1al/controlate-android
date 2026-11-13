package com.dur1al.controlate.porteria.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatenteTest {

    @Test
    fun `normalize uppercases and removes spaces, dots and dashes`() {
        assertEquals("ABCD12", Patente.normalize(" ab-cd.12 "))
    }

    @Test
    fun `isValid accepts 4 to 6 alphanumeric characters`() {
        assertTrue(Patente.isValid("DEMO01"))
        assertTrue(Patente.isValid("ab12"))
    }

    @Test
    fun `isValid rejects too short, too long or symbols`() {
        assertFalse(Patente.isValid("AB1"))
        assertFalse(Patente.isValid("ABCDEFG"))
        assertFalse(Patente.isValid("AB#12"))
    }
}
