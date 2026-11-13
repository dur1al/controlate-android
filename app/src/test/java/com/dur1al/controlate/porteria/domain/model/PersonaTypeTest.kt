package com.dur1al.controlate.porteria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonaTypeTest {

    @Test
    fun `fromCode resolves stable codes`() {
        assertEquals(PersonaType.FUNCIONARIO, PersonaType.fromCode("FUNCIONARIO"))
    }

    @Test
    fun `fromCode accepts legacy display names in any case`() {
        assertEquals(PersonaType.ESTUDIANTE, PersonaType.fromCode("Estudiante"))
        assertEquals(PersonaType.VISITA, PersonaType.fromCode("visita"))
    }

    @Test
    fun `fromCode defaults blank to VISITA and unknown to OTRO`() {
        assertEquals(PersonaType.VISITA, PersonaType.fromCode(null))
        assertEquals(PersonaType.VISITA, PersonaType.fromCode(" "))
        assertEquals(PersonaType.OTRO, PersonaType.fromCode("Contratista"))
    }
}
