package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.domain.model.BikeEntryInput
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.model.ValidationError.Field
import com.dur1al.controlate.porteria.domain.model.ValidationError.Reason
import com.dur1al.controlate.porteria.domain.model.VehicleEntryInput
import org.junit.Assert.assertEquals
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
}
