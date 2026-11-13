package com.dur1al.controlate.porteria.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ControlPointTest {

    @Test
    fun `factory points keep their persisted ids after renaming porterias`() {
        // Los ids están guardados en turnos y nombres de reportes: no deben cambiar.
        assertEquals(
            listOf("porteria_2" to "Portería 1", "porteria_3" to "Portería 2", "porteria_4" to "Portería 3",
                "bicicletero_1" to "Punto Bicicleta 1", "bicicletero_2" to "Punto Bicicleta 2"),
            ControlPoint.DEFAULTS.map { it.id to it.name }
        )
    }

    @Test
    fun `factory points are ordered from zero within each kind`() {
        ControlPoint.Kind.entries.forEach { kind ->
            val orders = ControlPoint.DEFAULTS.filter { it.kind == kind }.map { it.order }
            assertEquals(orders.indices.toList(), orders)
        }
    }

    @Test
    fun `kind resolves from its code and from legacy ids`() {
        assertEquals(ControlPoint.Kind.BICICLETERO, ControlPoint.Kind.fromCode(" bicicletero "))
        assertEquals(ControlPoint.Kind.PORTERIA, ControlPoint.Kind.fromId("porteria_7"))
        assertEquals(ControlPoint.Kind.BICICLETERO, ControlPoint.Kind.fromId("BICICLETERO_2"))
        assertNull(ControlPoint.Kind.fromId("legacy"))
        assertNull(ControlPoint.Kind.fromCode(null))
    }
}
