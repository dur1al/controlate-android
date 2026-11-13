package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.domain.model.BikeEntryInput
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.model.ValidationError.Field
import com.dur1al.controlate.porteria.domain.model.ValidationError.Reason
import com.dur1al.controlate.porteria.domain.model.VehicleEntryInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateRegistroUseCaseTest {

    private val useCase = ValidateRegistroUseCase()

    private fun vehicle(
        patente: String = "DEMO01",
        nombre: String = "Ana Torres",
        rut: String? = "11.111.111-1",
        esPadron: Boolean = false,
        padron: String? = null
    ) = VehicleEntryInput(
        patente = patente, nombre = nombre, rut = rut, departamento = "Adm",
        descripcionAuto = "Sedán", empresa = null, tipoPersona = PersonaType.VISITA,
        esPadron = esPadron, padron = padron, observaciones = null
    )

    @Test
    fun `valid vehicle entry has no errors`() {
        assertTrue(useCase.validateVehicle(vehicle()).isEmpty())
    }

    @Test
    fun `vehicle entry reports each invalid field with a reason code`() {
        val errors = useCase.validateVehicle(
            vehicle(patente = "A-1", nombre = " ", rut = "11.111.111-2", esPadron = true, padron = "")
        )
        assertEquals(
            setOf(
                ValidationError(Field.PATENTE, Reason.INVALID_FORMAT),
                ValidationError(Field.NOMBRE, Reason.REQUIRED),
                ValidationError(Field.RUT, Reason.INVALID_FORMAT),
                ValidationError(Field.PADRON, Reason.REQUIRED)
            ),
            errors.toSet()
        )
    }

    @Test
    fun `vehicle rut is optional but a partial rut is rejected`() {
        assertTrue(useCase.validateVehicle(vehicle(rut = null)).isEmpty())
        assertTrue(useCase.validateVehicle(vehicle(rut = "")).isEmpty())
        assertEquals(
            listOf(ValidationError(Field.RUT, Reason.INVALID_FORMAT)),
            useCase.validateVehicle(vehicle(rut = "12345"))
        )
    }

    @Test
    fun `one letter name is too short`() {
        assertEquals(
            listOf(ValidationError(Field.NOMBRE, Reason.TOO_SHORT)),
            useCase.validateVehicle(vehicle(nombre = "A"))
        )
    }

    @Test
    fun `bike entry requires valid rut, name and career`() {
        val errors = useCase.validateBike(
            BikeEntryInput(rut = "11.111.111-2", nombre = "", carrera = "", tipoPersona = PersonaType.ESTUDIANTE, observacion = null)
        )
        assertEquals(
            setOf(
                ValidationError(Field.RUT, Reason.INVALID_FORMAT),
                ValidationError(Field.NOMBRE, Reason.REQUIRED),
                ValidationError(Field.CARRERA, Reason.REQUIRED)
            ),
            errors.toSet()
        )
    }

    @Test
    fun `bike entry with blank rut reports required`() {
        val errors = useCase.validateBike(
            BikeEntryInput(rut = "", nombre = "Ana", carrera = "Ing", tipoPersona = PersonaType.ESTUDIANTE, observacion = null)
        )
        assertEquals(listOf(ValidationError(Field.RUT, Reason.REQUIRED)), errors)
    }

    // region QA por campo

    @Test
    fun `plate is required and then checked for format`() {
        assertEquals(ValidationError(Field.PATENTE, Reason.REQUIRED), useCase.patente("  "))
        assertEquals(ValidationError(Field.PATENTE, Reason.INVALID_FORMAT), useCase.patente("AB1"))
        assertEquals(ValidationError(Field.PATENTE, Reason.INVALID_FORMAT), useCase.patente("ABCDEFG"))
        assertEquals(ValidationError(Field.PATENTE, Reason.INVALID_FORMAT), useCase.patente("AB#12"))
        listOf("DEMO01", "ab-cd.12", "AB 1234", "XY12").forEach { assertNull("debe aceptar '$it'", useCase.patente(it)) }
    }

    @Test
    fun `names accept accents, enye, apostrophes and hyphens`() {
        listOf("José Pérez", "Ñuñoa Muñoz", "Bernardo O'Higgins", "Ana-María Soto", "J. Pérez", "Lu").forEach {
            assertNull("debe aceptar '$it'", useCase.nombre(it))
        }
    }

    @Test
    fun `names reject digits, symbols, only punctuation and excessive length`() {
        listOf("Juan 2", "Ana@Soto", "12345", "--", "'.'").forEach {
            assertEquals("debe rechazar '$it'", ValidationError(Field.NOMBRE, Reason.INVALID_FORMAT), useCase.nombre(it))
        }
        assertEquals(ValidationError(Field.NOMBRE, Reason.TOO_LONG), useCase.nombre("a".repeat(61)))
        assertNull(useCase.nombre("a".repeat(60)))
        assertEquals(ValidationError(Field.NOMBRE, Reason.REQUIRED), useCase.nombre("   "))
    }

    @Test
    fun `rut is optional or required depending on the form`() {
        assertNull(useCase.rut(null, required = false))
        assertNull(useCase.rut(" ", required = false))
        assertEquals(ValidationError(Field.RUT, Reason.REQUIRED), useCase.rut(" ", required = true))
        assertEquals(ValidationError(Field.RUT, Reason.INVALID_FORMAT), useCase.rut("12.345.678-4", required = false))
        assertEquals(ValidationError(Field.RUT, Reason.INVALID_FORMAT), useCase.rut("12K45678-9", required = true))
        assertNull(useCase.rut("12.345.678-5", required = true))
    }

    @Test
    fun `career is required and limited to 60 characters`() {
        assertEquals(ValidationError(Field.CARRERA, Reason.REQUIRED), useCase.carrera(""))
        assertEquals(ValidationError(Field.CARRERA, Reason.TOO_LONG), useCase.carrera("x".repeat(61)))
        assertNull(useCase.carrera("Ingeniería Civil"))
    }

    @Test
    fun `padron is only required when the checkbox is on`() {
        assertTrue(useCase.validateVehicle(vehicle(esPadron = false, padron = null)).isEmpty())
        assertTrue(useCase.validateVehicle(vehicle(esPadron = true, padron = "P-9")).isEmpty())
        assertEquals(listOf(ValidationError(Field.PADRON, Reason.REQUIRED)), useCase.validateVehicle(vehicle(esPadron = true, padron = " ")))
    }

    @Test
    fun `directory edit validates plate, name and optional rut`() {
        assertTrue(useCase.validatePersona("DEMO01", "Ana Torres", null).isEmpty())
        assertEquals(
            setOf(
                ValidationError(Field.PATENTE, Reason.INVALID_FORMAT),
                ValidationError(Field.NOMBRE, Reason.INVALID_FORMAT),
                ValidationError(Field.RUT, Reason.INVALID_FORMAT)
            ),
            useCase.validatePersona("A1", "Ana 2", "11.111.111-2").toSet()
        )
    }

    // endregion
}
