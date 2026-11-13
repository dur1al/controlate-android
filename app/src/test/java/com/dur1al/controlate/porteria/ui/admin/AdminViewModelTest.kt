package com.dur1al.controlate.porteria.ui.admin

import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.PointCount
import com.dur1al.controlate.porteria.domain.report.ReportExporter
import com.dur1al.controlate.porteria.domain.report.ReportLabels
import com.dur1al.controlate.porteria.domain.usecase.BuildReportUseCase
import com.dur1al.controlate.porteria.domain.usecase.ClearOperationalDataUseCase
import com.dur1al.controlate.porteria.domain.usecase.GetDailyStatsUseCase
import com.dur1al.controlate.porteria.testing.FakeBikeEntryRepository
import com.dur1al.controlate.porteria.testing.FakeClock
import com.dur1al.controlate.porteria.testing.FakeControlPointRepository
import com.dur1al.controlate.porteria.testing.FakePersonaRepository
import com.dur1al.controlate.porteria.testing.FakeShiftRepository
import com.dur1al.controlate.porteria.testing.FakeVehicleEntryRepository
import com.dur1al.controlate.porteria.testing.MainDispatcherRule
import com.dur1al.controlate.porteria.testing.Points
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class AdminViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val clock = FakeClock()
    private val vehicles = FakeVehicleEntryRepository()
    private val bikes = FakeBikeEntryRepository()
    private val shifts = FakeShiftRepository(vehicles, bikes)
    private val labels = object : ReportLabels {
        override fun unknownControlPoint(id: String) = id
        override fun personaType(code: String) = code
        override fun shiftType(code: String) = code
    }
    private val exported = mutableListOf<String>()

    private fun viewModel() = AdminViewModel(
        getDailyStats = GetDailyStatsUseCase(shifts, FakeControlPointRepository(), clock),
        clearOperationalData = ClearOperationalDataUseCase(shifts, vehicles, bikes),
        buildReport = BuildReportUseCase(shifts, vehicles, bikes, FakePersonaRepository(), FakeControlPointRepository(), labels, clock),
        exporter = ReportExporter { exported += it.fileStem; File(it.fileStem) }
    )

    @Test
    fun `general scope sums every point`() = runTest {
        shifts.pointCounts = listOf(PointCount("porteria_2", 3, 1), PointCount("bicicletero_1", 2, 0))
        val vm = viewModel()

        val state = vm.state.value
        assertEquals(5, state.points.size)
        assertEquals(AdminViewModel.Totals(5, 1, 4), state.totals)
    }

    @Test
    fun `selecting a point narrows the totals`() = runTest {
        shifts.pointCounts = listOf(PointCount("porteria_2", 3, 1), PointCount("bicicletero_1", 2, 0))
        val vm = viewModel()
        vm.selectScope(Points.BICICLETERO_1)
        assertEquals(AdminViewModel.Totals(2, 0, 2), vm.state.value.totals)
        vm.selectScope(null)
        assertEquals(AdminViewModel.Totals(5, 1, 4), vm.state.value.totals)
    }

    @Test
    fun `export summary shares the generated file`() = runTest {
        val vm = viewModel()
        vm.exportSummary()
        assertTrue(vm.events.first() is AdminViewModel.Event.Share)
        assertEquals(1, exported.size)
    }

    @Test
    fun `clearing data removes bike records too and refreshes stats`() = runTest {
        val shift = shifts.insert(Turno(guardia = "G", controlPoint = "bicicletero_1", tipo = "DAY", fecha = "x", iniciadoAt = 1, finalizadoAt = 2))
        bikes.insert(RegistroBicicleta(shiftId = shift, rut = "1", nombre = "A", carrera = "-", tipoPersona = "ESTUDIANTE", entradaAt = 1))
        val vm = viewModel()

        vm.clearOperationalData()

        assertTrue(bikes.rows.value.isEmpty())
        assertEquals(AdminViewModel.Event.Cleared, vm.events.first())
    }
}
