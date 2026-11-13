package com.dur1al.controlate.porteria.ui.db

import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.report.ReportExporter
import com.dur1al.controlate.porteria.domain.report.ReportLabels
import com.dur1al.controlate.porteria.domain.usecase.BuildReportUseCase
import com.dur1al.controlate.porteria.domain.usecase.ImportPersonasUseCase
import com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase
import com.dur1al.controlate.porteria.testing.FakeBikeEntryRepository
import com.dur1al.controlate.porteria.testing.FakeClock
import com.dur1al.controlate.porteria.testing.FakeControlPointRepository
import com.dur1al.controlate.porteria.testing.FakePersonaRepository
import com.dur1al.controlate.porteria.testing.FakeShiftRepository
import com.dur1al.controlate.porteria.testing.FakeVehicleEntryRepository
import com.dur1al.controlate.porteria.testing.MainDispatcherRule
import com.dur1al.controlate.porteria.ui.common.UiMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class DirectoryViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val ana = Persona(id = 1, patente = "DEMO01", nombreApellido = "Ana", departamento = "A", descripcionAuto = "-", rut = "111111111")
    private val luis = Persona(id = 2, patente = "DEMO02", nombreApellido = "Luis", departamento = "B", descripcionAuto = "-")
    private val personas = FakePersonaRepository(listOf(ana, luis))
    private val labels = object : ReportLabels {
        override fun unknownControlPoint(id: String) = id
        override fun personaType(code: String) = code
        override fun shiftType(code: String) = code
    }

    private fun viewModel(): DirectoryViewModel {
        val vehicles = FakeVehicleEntryRepository()
        val bikes = FakeBikeEntryRepository()
        return DirectoryViewModel(
            personas = personas,
            importPersonas = ImportPersonasUseCase(personas),
            buildReport = BuildReportUseCase(FakeShiftRepository(vehicles, bikes), vehicles, bikes, personas, FakeControlPointRepository(), labels, FakeClock()),
            exporter = ReportExporter { File(it.fileStem) },
            validator = ValidateRegistroUseCase()
        )
    }

    @Test
    fun `update normalizes plate and rut`() = runTest {
        val vm = viewModel()
        vm.update(ana.copy(patente = "demo 09", rut = "11.111.111-1"))

        assertEquals(DirectoryViewModel.Event.EditSaved, vm.events.first())
        assertEquals(DirectoryViewModel.Event.Message(UiMessage(R.string.directory_updated)), vm.events.first())
        assertEquals("DEMO09", personas.getByRut("111111111")?.patente)
    }

    @Test
    fun `update with an invalid plate is rejected without saving`() = runTest {
        val vm = viewModel()
        vm.update(ana.copy(patente = "A1"))

        assertEquals(listOf(ValidationError(ValidationError.Field.PATENTE, ValidationError.Reason.INVALID_FORMAT)), vm.editErrors.value)
        assertEquals("DEMO01", personas.rows.value.first { it.id == 1 }.patente)
    }

    @Test
    fun `update with a plate owned by someone else reports it instead of crashing`() = runTest {
        val vm = viewModel()
        vm.update(ana.copy(patente = "DEMO02"))

        assertEquals(listOf(ValidationError(ValidationError.Field.PATENTE, ValidationError.Reason.DUPLICATE)), vm.editErrors.value)
        assertEquals("DEMO01", personas.rows.value.first { it.id == 1 }.patente)
    }

    @Test
    fun `update rejects an invalid rut and name and keeps the original row`() = runTest {
        val vm = viewModel()
        vm.update(ana.copy(nombreApellido = "Ana 2", rut = "12K45678-5"))

        assertEquals(
            setOf(
                ValidationError(ValidationError.Field.NOMBRE, ValidationError.Reason.INVALID_FORMAT),
                ValidationError(ValidationError.Field.RUT, ValidationError.Reason.INVALID_FORMAT)
            ),
            vm.editErrors.value.toSet()
        )
        assertEquals(ana, personas.rows.value.first { it.id == 1 })
    }

    @Test
    fun `clearing the rut on edit stores it as null`() = runTest {
        val vm = viewModel()
        vm.update(ana.copy(rut = ""))

        assertEquals(DirectoryViewModel.Event.EditSaved, vm.events.first())
        assertNull(personas.rows.value.first { it.id == 1 }.rut)
    }

    @Test
    fun `starting a new edit and fixing a field clear previous errors`() = runTest {
        val vm = viewModel()
        vm.update(ana.copy(patente = "A1", nombreApellido = ""))
        vm.clearEditError(ValidationError.Field.PATENTE)
        assertEquals(listOf(ValidationError.Field.NOMBRE), vm.editErrors.value.map { it.field })

        vm.startEdit()
        assertTrue(vm.editErrors.value.isEmpty())
    }
}
