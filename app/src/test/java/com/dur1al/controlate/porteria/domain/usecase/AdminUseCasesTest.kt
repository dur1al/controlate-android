package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.importer.PersonaCsvParser
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.PointCount
import com.dur1al.controlate.porteria.domain.model.PointStat
import com.dur1al.controlate.porteria.domain.time.TimeFormat
import com.dur1al.controlate.porteria.testing.FakeBikeEntryRepository
import com.dur1al.controlate.porteria.testing.FakeClock
import com.dur1al.controlate.porteria.testing.FakeControlPointRepository
import com.dur1al.controlate.porteria.testing.FakePersonaRepository
import com.dur1al.controlate.porteria.testing.FakeSessionStore
import com.dur1al.controlate.porteria.testing.FakeShiftRepository
import com.dur1al.controlate.porteria.testing.FakeVehicleEntryRepository
import com.dur1al.controlate.porteria.testing.Points
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AdminUseCasesTest {

    private val zone = ZoneId.of("America/Santiago")
    private val clock = FakeClock(LocalDateTime.of(2026, 6, 14, 15, 0).atZone(zone).toInstant().toEpochMilli())
    private val vehicles = FakeVehicleEntryRepository()
    private val bikes = FakeBikeEntryRepository()
    private val shifts = FakeShiftRepository(vehicles, bikes)
    private val session = FakeSessionStore()

    @Test
    fun `daily stats cover every control point for the local day`() = runTest {
        shifts.pointCounts = listOf(PointCount("porteria_3", 5, 2), PointCount("BICICLETERO_2", 4, 4), PointCount("legacy", 9, 9))

        val stats = GetDailyStatsUseCase(shifts, FakeControlPointRepository(), clock, zone)()

        assertEquals(TimeFormat.dayRange(LocalDate.of(2026, 6, 14), zone), shifts.lastCountRange)
        assertEquals(
            listOf(
                PointStat(Points.PORTERIA_1, 0, 0),
                PointStat(Points.PORTERIA_2, 5, 2),
                PointStat(Points.PORTERIA_3, 0, 0),
                PointStat(Points.BICICLETERO_1, 0, 0),
                PointStat(Points.BICICLETERO_2, 4, 4)
            ),
            stats
        )
        assertEquals(3, stats[1].dentro)
    }

    @Test
    fun `clearing operational data removes both kinds of records but keeps the active shift`() = runTest {
        val closed = shifts.insert(Turno(guardia = "A", controlPoint = "porteria_2", tipo = "DAY", fecha = "x", iniciadoAt = 1, finalizadoAt = 2))
        val active = shifts.insert(Turno(guardia = "B", controlPoint = "bicicletero_1", tipo = "DAY", fecha = "x", iniciadoAt = 3))
        session.setActiveShift(active)
        vehicles.insert(RegistroIngreso(shiftId = closed, patente = "DEMO01", nombreApellido = "A", departamento = "-", descripcionAuto = "-", tipoPersona = "VISITA", entradaAt = 1))
        bikes.insert(RegistroBicicleta(shiftId = active, rut = "111111111", nombre = "B", carrera = "-", tipoPersona = "ESTUDIANTE", entradaAt = 3))

        ClearOperationalDataUseCase(shifts, vehicles, bikes)()

        assertTrue(vehicles.rows.value.isEmpty())
        assertTrue("bike records must also be cleared", bikes.rows.value.isEmpty())
        assertEquals(setOf(active), shifts.turnos.value.keys)
    }

    @Test
    fun `import stores rows only when the whole file is valid`() = runTest {
        val personas = FakePersonaRepository()
        val import = ImportPersonasUseCase(personas)

        val failed = import("DEMO01,Ana\nAB1,Luis")
        assertEquals(listOf(PersonaCsvParser.LineError(2, PersonaCsvParser.LineError.Reason.INVALID_PLATE)), failed.errors)
        assertTrue(personas.rows.value.isEmpty())

        val ok = import("DEMO01,Ana\nDEMO02,Luis")
        assertEquals(2, ok.personas.size)
        assertEquals(listOf("DEMO01", "DEMO02"), personas.rows.value.map { it.patente })
    }

    @Test
    fun `inactive points only appear when they had movement and new points appear with zeros`() = runTest {
        val points = FakeControlPointRepository()
        points.update(Points.PORTERIA_1.copy(active = false))
        points.update(Points.PORTERIA_2.copy(active = false))
        points.insert(ControlPoint("porteria_5", "Portería 4", ControlPoint.Kind.PORTERIA, 3))
        shifts.pointCounts = listOf(PointCount("porteria_3", 1, 0))

        val stats = GetDailyStatsUseCase(shifts, points, clock, zone)()

        assertEquals(
            listOf("porteria_3", "porteria_4", "porteria_5", "bicicletero_1", "bicicletero_2"),
            stats.map { it.controlPoint.id }
        )
        assertEquals(1, stats.first().entradas)
    }
}
