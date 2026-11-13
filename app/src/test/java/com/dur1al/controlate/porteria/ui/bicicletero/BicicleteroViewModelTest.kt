package com.dur1al.controlate.porteria.ui.bicicletero

import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.domain.model.BikeEntryInput
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.model.ShiftType
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.report.ReportExporter
import com.dur1al.controlate.porteria.domain.report.ReportLabels
import com.dur1al.controlate.porteria.domain.usecase.BuildReportUseCase
import com.dur1al.controlate.porteria.domain.usecase.EndShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.FindPersonaUseCase
import com.dur1al.controlate.porteria.domain.usecase.GetActiveShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.MarkExitUseCase
import com.dur1al.controlate.porteria.domain.usecase.RegisterBikeEntryUseCase
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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.File

class BicicleteroViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val clock = FakeClock()
    private val session = FakeSessionStore()
    private val vehicles = FakeVehicleEntryRepository()
    private val bikes = FakeBikeEntryRepository()
    private val shifts = FakeShiftRepository(vehicles, bikes)
    private val personas = FakePersonaRepository()
    private val exported = mutableListOf<String>()
    private val labels = object : ReportLabels {
        override fun unknownControlPoint(id: String) = id
        override fun personaType(code: String) = code
        override fun shiftType(code: String) = code
    }

    private suspend fun viewModel(): BicicleteroViewModel {
        StartShiftUseCase(shifts, session, clock)("Guardia", Points.BICICLETERO_1, ShiftType.DAY)
        return BicicleteroViewModel(
            getActiveShift = GetActiveShiftUseCase(shifts, session),
            registerEntry = RegisterBikeEntryUseCase(ValidateRegistroUseCase(), bikes, clock),
            markExit = MarkExitUseCase(vehicles, bikes, clock),
            findPersona = FindPersonaUseCase(personas, bikes),
            entries = bikes,
            closeShift = EndShiftUseCase(shifts, session, clock),
            buildReport = BuildReportUseCase(shifts, vehicles, bikes, personas, FakeControlPointRepository(), labels, clock),
            exporter = ReportExporter { exported += it.fileStem; File(it.fileStem) },
            validator = ValidateRegistroUseCase(),
            controlPoints = FakeControlPointRepository()
        )
    }

    private fun input(rut: String) = BikeEntryInput(rut, "Ana", "Ing", PersonaType.ESTUDIANTE, null)

    @Test
    fun `registers bikes and searches by rut or name`() = runTest {
        val vm = viewModel()
        vm.register(input("11.111.111-1"))
        assertEquals(BicicleteroViewModel.FormEvent.Saved, vm.formEvents.first())
        vm.register(input("12.345.678-5"))

        assertEquals(2, vm.state.value.registros.size)
        vm.setQuery("12.345")
        assertEquals(listOf("123456785"), vm.state.value.registros.map { it.rut })
    }

    @Test
    fun `exit can be undone`() = runTest {
        val vm = viewModel()
        vm.register(input("11.111.111-1"))
        assertEquals(BicicleteroViewModel.Event.Message(UiMessage(R.string.entry_registered)), vm.events.first())
        val registro = vm.state.value.registros.single()

        vm.markExit(registro)
        val event = vm.events.first() as BicicleteroViewModel.Event.ExitMarked
        vm.undoExit(event.id, event.at)

        assertNull(vm.state.value.registros.single().salidaAt)
    }

    @Test
    fun `registering a rut with a bike inside shows a field error`() = runTest {
        val vm = viewModel()
        vm.register(input("11.111.111-1"))
        vm.register(input("11.111.111-1"))

        assertEquals(listOf(ValidationError(ValidationError.Field.RUT, ValidationError.Reason.ALREADY_INSIDE)), vm.formErrors.value)
        assertEquals(1, vm.state.value.registros.size)
        assertFalse(vm.formSaving.value)
    }

    @Test
    fun `rut lookup autofills name and career`() = runTest {
        val vm = viewModel()
        vm.register(input("11.111.111-1"))
        vm.formEvents.first()

        vm.lookupRut("11.111.111-1")

        assertEquals(
            BicicleteroViewModel.FormEvent.Autofill(FindPersonaUseCase.BikeAutofill("Ana", "Ing", PersonaType.ESTUDIANTE)),
            vm.formEvents.first()
        )
    }

    @Test
    fun `end shift saves the report and closes the shift`() = runTest {
        val vm = viewModel()
        vm.endShift()
        assertEquals(BicicleteroViewModel.Event.ShiftEnded, vm.events.first())
        assertEquals(1, exported.size)
        assertNull(session.activeShiftId())
    }
}
