package com.dur1al.controlate.porteria.domain.importer

import com.dur1al.controlate.porteria.domain.importer.PersonaCsvParser.LineError
import com.dur1al.controlate.porteria.domain.importer.PersonaCsvParser.LineError.Reason
import com.dur1al.controlate.porteria.domain.model.PersonaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaCsvParserTest {

    @Test
    fun `parses all columns and normalizes plate, rut and type`() {
        val result = PersonaCsvParser.parse(
            "demo 01, Ana Torres, Adm, 11.111.111-1, P-9, ACME, Sedán Gris, funcionario"
        )
        assertTrue(result.errors.isEmpty())
        val p = result.personas.single()
        assertEquals("DEMO01", p.patente)
        assertEquals("Ana Torres", p.nombreApellido)
        assertEquals("Adm", p.departamento)
        assertEquals("111111111", p.rut)
        assertEquals("P-9", p.padron)
        assertEquals("ACME", p.empresa)
        assertEquals("Sedán Gris", p.descripcionAuto)
        assertEquals(PersonaType.FUNCIONARIO.name, p.tipoPersona)
    }

    @Test
    fun `optional columns get defaults`() {
        val p = PersonaCsvParser.parse("TEST10,Lucía Mendoza").personas.single()
        assertEquals("-", p.departamento)
        assertEquals("-", p.descripcionAuto)
        assertNull(p.rut)
        assertNull(p.empresa)
        assertEquals(PersonaType.FUNCIONARIO.name, p.tipoPersona)
    }

    @Test
    fun `skips blank lines and an optional header row`() {
        val result = PersonaCsvParser.parse("PATENTE,NOMBRE\n\nDEMO01,Ana\n")
        assertEquals(1, result.personas.size)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `reports errors with 1-based line numbers`() {
        val result = PersonaCsvParser.parse("SOLO\nAB1,Ana\nDEMO02, \nDEMO03,Ana,x,11.111.111-2")
        assertEquals(
            listOf(
                LineError(1, Reason.MISSING_FIELDS),
                LineError(2, Reason.INVALID_PLATE),
                LineError(3, Reason.MISSING_NAME),
                LineError(4, Reason.INVALID_RUT)
            ),
            result.errors
        )
        assertTrue(result.personas.isEmpty())
    }

    @Test
    fun `duplicate plates inside the same file keep the last occurrence`() {
        val result = PersonaCsvParser.parse("DEMO01,Ana\nDEMO01,Ana Torres")
        assertEquals("Ana Torres", result.personas.single().nombreApellido)
    }

    @Test
    fun `rut column is validated as written, not after cleaning it`() {
        // Antes se normalizaba primero y una K en el cuerpo desaparecía en silencio.
        val result = PersonaCsvParser.parse("DEMO01,Ana Torres,Adm,12K45678-5")
        assertTrue(result.personas.isEmpty())
        assertEquals(listOf(LineError(1, Reason.INVALID_RUT)), result.errors)
    }

    @Test
    fun `rut column accepts dotted, dashed or plain formats`() {
        val result = PersonaCsvParser.parse("DEMO01,Ana,A,12.345.678-5\nDEMO02,Luis,B,12345678-5\nDEMO03,Eva,C,123456785")
        assertTrue(result.errors.isEmpty())
        assertEquals(listOf("123456785", "123456785", "123456785"), result.personas.map { it.rut })
    }
}
