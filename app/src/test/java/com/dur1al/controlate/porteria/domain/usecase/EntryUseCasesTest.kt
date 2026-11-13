package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.domain.model.BikeEntryInput
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.model.VehicleEntryInput
import com.dur1al.controlate.porteria.testing.FakeBikeEntryRepository
import com.dur1al.controlate.porteria.testing.FakeClock
import com.dur1al.controlate.porteria.testing.FakePersonaRepository
import com.dur1al.controlate.porteria.testing.FakeVehicleEntryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryUseCasesTest {

    private val clock = FakeClock()
    private val vehicles = FakeVehicleEntryRepository()
    private val bikes = FakeBikeEntryRepository()
    private val existing = Persona(
        id = 1, patente = "DEMO01", nombreApellido = "Ana Torres", departamento = "Adm",
        descripcionAuto = "Sedán Gris", rut = "111111111", tipoPersona = "FUNCIONARIO"
    )
    private val personas = FakePersonaRepository(listOf(existing))
    private val validator = ValidateRegistroUseCase()

    private val registerVehicle = RegisterVehicleEntryUseCase(validator, vehicles, personas, clock)
    private val registerBike = RegisterBikeEntryUseCase(validator, bikes, clock)
    private val markExit = MarkExitUseCase(vehicles, bikes, clock)
    private val findPersona = FindPersonaUseCase(personas, bikes)

    private fun vehicleInput(patente: String = "demo 01", rut: String? = "11.111.111-1", nombre: String = "Ana Torres") =
        VehicleEntryInput(
            patente = patente, nombre = nombre, rut = rut, departamento = "Adm", descripcionAuto = "Sedán",
            empresa = " ", tipoPersona = PersonaType.FUNCIONARIO, esPadron = false, padron = null, observaciones = ""
        )

    @Test
    fun `vehicle entry is stored normalized with the current time`() = runTest {
        val outcome = registerVehicle(shiftId = 7, input = vehicleInput())

        assertTrue(outcome is RegisterEntryOutcome.Saved)
        val saved = vehicles.rows.value.single()
        assertEquals(7, saved.shiftId)
        assertEquals("DEMO01", saved.patente)
        assertEquals("111111111", saved.rut)
        assertEquals("FUNCIONARIO", saved.tipoPersona)
        assertEquals(clock.nowMs, saved.entradaAt)
        assertNull(saved.empresa)
        assertNull(saved.observaciones)
    }

    @Test
    fun `invalid vehicle entry is not stored`() = runTest {
        val outcome = registerVehicle(shiftId = 7, input = vehicleInput(patente = "A1"))
        assertEquals(
            RegisterEntryOutcome.Invalid(listOf(ValidationError(ValidationError.Field.PATENTE, ValidationError.Reason.INVALID_FORMAT))),
            outcome
        )
        assertTrue(vehicles.rows.value.isEmpty())
    }

    @Test
    fun `unknown rut with a known plate does not overwrite the directory entry`() = runTest {
        // Regresión: antes la búsqueda por patente se omitía cuando venía RUT y la persona se reemplazaba.
        registerVehicle(shiftId = 7, input = vehicleInput(patente = "DEMO01", rut = "12.345.678-5", nombre = "Otro Nombre"))

        assertEquals(listOf(existing), personas.rows.value)
    }

    @Test
    fun `plate already inside the shift is rejected until it exits`() = runTest {
        val first = registerVehicle(shiftId = 7, input = vehicleInput()) as RegisterEntryOutcome.Saved

        assertEquals(
            RegisterEntryOutcome.Invalid(listOf(ValidationError(ValidationError.Field.PATENTE, ValidationError.Reason.ALREADY_INSIDE))),
            registerVehicle(shiftId = 7, input = vehicleInput(patente = "DEMO-01"))
        )
        // Otro turno no se ve afectado.
        assertTrue(registerVehicle(shiftId = 8, input = vehicleInput()) is RegisterEntryOutcome.Saved)

        markExit.vehicle(first.id)
        assertTrue(registerVehicle(shiftId = 7, input = vehicleInput()) is RegisterEntryOutcome.Saved)
    }

    @Test
    fun `rut with a bike inside the shift is rejected until it exits`() = runTest {
        val input = BikeEntryInput("11.111.111-1", "Ana", "Ing", PersonaType.ESTUDIANTE, null)
        val first = registerBike(shiftId = 3, input = input) as RegisterEntryOutcome.Saved

        assertEquals(
            RegisterEntryOutcome.Invalid(listOf(ValidationError(ValidationError.Field.RUT, ValidationError.Reason.ALREADY_INSIDE))),
            registerBike(shiftId = 3, input = input.copy(rut = "111111111"))
        )

        markExit.bike(first.id)
        assertTrue(registerBike(shiftId = 3, input = input) is RegisterEntryOutcome.Saved)
    }

    @Test
    fun `new person is added to the directory`() = runTest {
        registerVehicle(shiftId = 7, input = vehicleInput(patente = "NEW123", rut = "12.345.678-5", nombre = "Luis Soto"))

        val added = personas.rows.value.single { it.patente == "NEW123" }
        assertEquals("Luis Soto", added.nombreApellido)
        assertEquals("123456785", added.rut)
    }

    @Test
    fun `bike entry is stored with canonical rut`() = runTest {
        val outcome = registerBike(
            shiftId = 3,
            input = BikeEntryInput("11.111.111-1", " Ana ", "Ingeniería", PersonaType.ESTUDIANTE, " ")
        )
        assertTrue(outcome is RegisterEntryOutcome.Saved)
        val saved = bikes.rows.value.single()
        assertEquals("111111111", saved.rut)
        assertEquals("Ana", saved.nombre)
        assertEquals("ESTUDIANTE", saved.tipoPersona)
        assertNull(saved.observacion)
        assertEquals(clock.nowMs, saved.entradaAt)
    }

    @Test
    fun `vehicle exit is marked once and can be undone`() = runTest {
        registerVehicle(shiftId = 7, input = vehicleInput())
        val id = vehicles.rows.value.single().id
        clock.advanceMinutes(30)

        val at = markExit.vehicle(id)
        assertEquals(clock.nowMs, at)
        assertNull("second exit is ignored", markExit.vehicle(id))

        assertTrue(markExit.undoVehicle(id, at!!))
        assertNull(vehicles.rows.value.single().salidaAt)
    }

    @Test
    fun `bike exit is marked once and can be undone`() = runTest {
        registerBike(3, BikeEntryInput("11.111.111-1", "Ana", "Ing", PersonaType.ESTUDIANTE, null))
        val id = bikes.rows.value.single().id

        val at = markExit.bike(id)!!
        assertNull(markExit.bike(id))
        assertTrue(markExit.undoBike(id, at))
        assertFalse(markExit.undoBike(id, at))
    }

    @Test
    fun `find by plate requires four characters and normalizes`() = runTest {
        assertNull(findPersona.byPatente("dem"))
        assertEquals(existing, findPersona.byPatente("demo-01"))
    }

    @Test
    fun `find by rut only searches complete valid ruts`() = runTest {
        assertNull(findPersona.byRut("11.111"))
        assertEquals(existing, findPersona.byRut("11.111.111-1"))
    }

    @Test
    fun `bike autofill prefers the last bike record over the directory`() = runTest {
        assertEquals(
            FindPersonaUseCase.BikeAutofill("Ana Torres", "Adm", PersonaType.FUNCIONARIO),
            findPersona.bikeAutofill("11.111.111-1")
        )
        bikes.insert(
            RegistroBicicleta(shiftId = 1, rut = "111111111", nombre = "Ana T.", carrera = "Derecho",
                tipoPersona = "ESTUDIANTE", entradaAt = 5)
        )
        assertEquals(
            FindPersonaUseCase.BikeAutofill("Ana T.", "Derecho", PersonaType.ESTUDIANTE),
            findPersona.bikeAutofill("111111111")
        )
    }
}
