package com.dur1al.controlate.porteria.ui.porteria

import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.model.ShiftType
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.model.VehicleEntryInput
import com.dur1al.controlate.porteria.domain.report.ReportExporter
import com.dur1al.controlate.porteria.domain.report.ReportLabels
import com.dur1al.controlate.porteria.domain.usecase.BuildReportUseCase
import com.dur1al.controlate.porteria.domain.usecase.EndShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.FindPersonaUseCase
import com.dur1al.controlate.porteria.domain.usecase.GetActiveShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.MarkExitUseCase
import com.dur1al.controlate.porteria.domain.usecase.RegisterVehicleEntryUseCase
import com.dur1al.controlate.porteria.domain.usecase.StartShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase
import com.dur1al.controlate.porteria.testing.FakeBikeEntryRepository
import com.dur1al.controlate.porteria.testing.FakeClock
import com.dur1al.controlate.porteria.testing.FakeControlPointRepository
import com.dur1al.controlate.porteria.testing.FakePersonaRepository
import com.dur1al.controlate.porteria.testing.FakeSessionStore
import com.dur1al.controlate.porteria.testing.FakeShiftRepository
import com.dur1al.controlate.porteria.testing.FakeVehicleEntryRepository
import com.dur1al.controlate.porteria.testing.MainDispatcherRule
import com.dur1al.controlate.porteria.testing.Points
import com.dur1al.controlate.porteria.ui.common.UiMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class PorteriaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val clock = FakeClock()
    private val session = FakeSessionStore()
    private val vehicles = FakeVehicleEntryRepository()
    private val bikes = FakeBikeEntryRepository()
    private val shifts = FakeShiftRepository(vehicles, bikes)
    private val personas = FakePersonaRepository(
        listOf(Persona(id = 1, patente = "DEMO01", nombreApellido = "Ana Torres", departamento = "Adm", descripcionAuto = "Sedán", rut = "111111111", tipoPersona = "FUNCIONARIO"))
    )
    private val exported = mutableListOf<String>()
    private val labels = object : ReportLabels {
        override fun unknownControlPoint(id: String) = id
        override fun personaType(code: String) = code
        override fun shiftType(code: String) = code
    }

    private suspend fun openShift(): Long =
        (StartShiftUseCase(shifts, session, clock)("Guardia", Points.PORTERIA_1, ShiftType.DAY) as StartShiftUseCase.Outcome.Started).turno.id

    private fun viewModel() = PorteriaViewModel(
        getActiveShift = GetActiveShiftUseCase(shifts, session),
        registerEntry = RegisterVehicleEntryUseCase(ValidateRegistroUseCase(), vehicles, personas, clock),
        markExit = MarkExitUseCase(vehicles, bikes, clock),
        findPersona = FindPersonaUseCase(personas, bikes),
        entries = vehicles,
        closeShift = EndShiftUseCase(shifts, session, clock),
        buildReport = BuildReportUseCase(shifts, vehicles, bikes, personas, FakeControlPointRepository(), labels, clock),
        exporter = ReportExporter { doc -> exported += doc.fileStem; File(doc.fileStem) },
        validator = ValidateRegistroUseCase(),
        controlPoints = FakeControlPointRepository()
    )

    private fun input(patente: String = "NEW123") = VehicleEntryInput(
        patente = patente, nombre = "Luis Soto", rut = null, departamento = "Ops", descripcionAuto = "-",
        empresa = null, tipoPersona = PersonaType.VISITA, esPadron = false, padron = null, observaciones = null
    )

    @Test
    fun `without an open shift the screen asks to go back to login`() = runTest {
        val vm = viewModel()
        assertEquals(PorteriaViewModel.Event.NoActiveShift, vm.events.first())
    }

    @Test
    fun `lists records of the active shift and filters by query`() = runTest {
        openShift()
        val vm = viewModel()
        vm.register(input("DEMO01"))
        vm.register(input("TEST10"))

        assertEquals(2, vm.state.value.registros.size)
        vm.setQuery("test")
        assertEquals(listOf("TEST10"), vm.state.value.registros.map { it.patente })
    }

    @Test
    fun `invalid entry exposes field errors and is not saved`() = runTest {
        openShift()
        val vm = viewModel()
        vm.register(input("X"))

        assertEquals(listOf(ValidationError(ValidationError.Field.PATENTE, ValidationError.Reason.INVALID_FORMAT)), vm.formErrors.value)
        assertTrue(vehicles.rows.value.isEmpty())
    }

    @Test
    fun `valid entry clears errors and notifies the form`() = runTest {
        openShift()
        val vm = viewModel()
        vm.register(input("X"))
        vm.register(input())

        assertEquals(emptyList<ValidationError>(), vm.formErrors.value)
        assertEquals(PorteriaViewModel.FormEvent.Saved, vm.formEvents.first())
    }

    @Test
    fun `marking an exit offers undo and undo reverts it`() = runTest {
        openShift()
        val vm = viewModel()
        vm.register(input())
        assertEquals(PorteriaViewModel.Event.Message(UiMessage(R.string.entry_registered)), vm.events.first())
        val registro = vm.state.value.registros.single()

        vm.markExit(registro)
        val event = vm.events.first() as PorteriaViewModel.Event.ExitMarked
        assertEquals(registro.patente, event.patente)
        assertEquals(clock.nowMs, vm.state.value.registros.single().salidaAt)

        vm.undoExit(event.id, event.at)
        assertNull(vm.state.value.registros.single().salidaAt)
    }

    @Test
    fun `saving confirms on the screen and re-enables the form`() = runTest {
        openShift()
        val vm = viewModel()
        vm.register(input())

        assertEquals(PorteriaViewModel.Event.Message(UiMessage(R.string.entry_registered)), vm.events.first())
        assertFalse(vm.formSaving.value)
    }

    @Test
    fun `registering a plate that is already inside shows a field error`() = runTest {
        openShift()
        val vm = viewModel()
        vm.register(input())
        vm.register(input())

        assertEquals(listOf(ValidationError(ValidationError.Field.PATENTE, ValidationError.Reason.ALREADY_INSIDE)), vm.formErrors.value)
        assertEquals(1, vehicles.rows.value.size)
    }

    @Test
    fun `a storage failure is reported in the form instead of crashing`() = runTest {
        openShift()
        val vm = viewModel()
        vehicles.failInsert = true
        vm.register(input())

        assertEquals(PorteriaViewModel.FormEvent.Failed(UiMessage(R.string.error_save_entry)), vm.formEvents.first())
        assertFalse(vm.formSaving.value)
    }

    @Test
    fun `fixing a field clears only its own error`() = runTest {
        openShift()
        val vm = viewModel()
        vm.register(input("X").copy(nombre = ""))
        assertEquals(2, vm.formErrors.value.size)

        vm.clearFieldError(ValidationError.Field.PATENTE)

        assertEquals(listOf(ValidationError.Field.NOMBRE), vm.formErrors.value.map { it.field })
    }

    @Test
    fun `the same person is autofilled once per open form`() = runTest {
        openShift()
        val vm = viewModel()
        val received = mutableListOf<PorteriaViewModel.FormEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.formEvents.toList(received) }

        vm.onFormOpened()
        vm.lookupPlate("DEMO01")
        vm.lookupRut("11.111.111-1")
        assertEquals(1, received.size)

        vm.onFormOpened()
        vm.lookupPlate("DEMO01")
        assertEquals(2, received.size)
    }

    @Test
    fun `plate lookup autofills from the directory`() = runTest {
        openShift()
        val vm = viewModel()
        vm.lookupPlate("demo01")
        assertEquals(PorteriaViewModel.FormEvent.Autofill(personas.rows.value.single()), vm.formEvents.first())
    }

    @Test
    fun `ending the shift saves its report and closes it`() = runTest {
        val id = openShift()
        val vm = viewModel()
        vm.endShift()

        assertEquals(PorteriaViewModel.Event.ShiftEnded, vm.events.first())
        assertEquals(1, exported.size)
        assertTrue(shifts.get(id)!!.finalizadoAt != null)
        assertNull(session.activeShiftId())
    }

    @Test
    fun `ending the shift still closes it when the report fails`() = runTest {
        val id = openShift()
        val vm = PorteriaViewModel(
            GetActiveShiftUseCase(shifts, session),
            RegisterVehicleEntryUseCase(ValidateRegistroUseCase(), vehicles, personas, clock),
            MarkExitUseCase(vehicles, bikes, clock), FindPersonaUseCase(personas, bikes), vehicles,
            EndShiftUseCase(shifts, session, clock),
            BuildReportUseCase(shifts, vehicles, bikes, personas, FakeControlPointRepository(), labels, clock),
            ReportExporter { error("disk full") },
            ValidateRegistroUseCase(),
            FakeControlPointRepository()
        )
        vm.endShift()
        assertEquals(PorteriaViewModel.Event.Message(UiMessage(R.string.error_generic)), vm.events.first())
        assertEquals(PorteriaViewModel.Event.ShiftEnded, vm.events.first())
        assertTrue(shifts.get(id)!!.finalizadoAt != null)
    }

    @Test
    fun `leaving a field shows format errors but not required ones`() = runTest {
        openShift()
        val vm = viewModel()

        vm.onFieldBlur(ValidationError.Field.NOMBRE, "  ")
        assertTrue(vm.formErrors.value.isEmpty())

        vm.onFieldBlur(ValidationError.Field.RUT, "12.345.678-4")
        vm.onFieldBlur(ValidationError.Field.PATENTE, "AB1")
        assertEquals(
            setOf(
                ValidationError(ValidationError.Field.RUT, ValidationError.Reason.INVALID_FORMAT),
                ValidationError(ValidationError.Field.PATENTE, ValidationError.Reason.INVALID_FORMAT)
            ),
            vm.formErrors.value.toSet()
        )

        vm.onFieldBlur(ValidationError.Field.RUT, "12.345.678-5")
        assertEquals(listOf(ValidationError.Field.PATENTE, ValidationError.Field.RUT), vm.formErrors.value.map { it.field }.sorted())
    }
}
