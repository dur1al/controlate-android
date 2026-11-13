package com.dur1al.controlate.porteria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftTypeTest {

    @Test
    fun `fromCode resolves codes and legacy labels`() {
        assertEquals(ShiftType.NIGHT, ShiftType.fromCode("NIGHT"))
        assertEquals(ShiftType.NIGHT, ShiftType.fromCode("NOCHE (20:00 - 08:00)"))
        assertEquals(ShiftType.DAY, ShiftType.fromCode("DIA (08:00 - 20:00)"))
    }

    @Test
    fun `fromCode defaults to DAY when unknown`() {
        assertEquals(ShiftType.DAY, ShiftType.fromCode("No definido"))
        assertEquals(ShiftType.DAY, ShiftType.fromCode(null))
    }
}
