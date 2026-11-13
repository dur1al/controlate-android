package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.PointStat
import com.dur1al.controlate.porteria.domain.report.ReportDocument
import com.dur1al.controlate.porteria.domain.report.ReportDocument.Column
import com.dur1al.controlate.porteria.domain.report.ReportDocument.MetaField
import com.dur1al.controlate.porteria.domain.report.ReportLabels
import com.dur1al.controlate.porteria.testing.FakeBikeEntryRepository
import com.dur1al.controlate.porteria.testing.FakeClock
import com.dur1al.controlate.porteria.testing.FakePersonaRepository
import com.dur1al.controlate.porteria.testing.FakeShiftRepository
import com.dur1al.controlate.porteria.testing.FakeVehicleEntryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BuildReportUseCaseTest {

    private val zone = ZoneId.of("America/Santiago")
    private fun at(d: Int, h: Int, m: Int) = LocalDateTime.of(2026, 6, d, h, m).atZone(zone).toInstant().toEpochMilli()

    private val labels = object : ReportLabels {
        override fun controlPoint(id: String) = "Punto[$id]"
        override fun personaType(code: String) = "Tipo[$code]"
        override fun shiftType(code: String) = "Turno[$code]"
    }
    private val clock = FakeClock(at(15, 9, 0))
    private val vehicles = FakeVehicleEntryRepository()
    private val bikes = FakeBikeEntryRepository()
    private val shifts = FakeShiftRepository(vehicles, bikes)
    private val personas = FakePersonaRepository()
    private val useCase = BuildReportUseCase(shifts, vehicles, bikes, personas, labels, clock, zone)

    @Test
    fun `vehicle shift report lists entries with formatted rut and times`() = runTest {
        val shiftId = shifts.insert(Turno(guardia = "Guardia Noche", controlPoint = "porteria_2", tipo = "NIGHT", fecha = "2026-06-14", iniciadoAt = at(14, 20, 0)))
        vehicles.insert(
            RegistroIngreso(shiftId = shiftId, patente = "DEMO01", nombreApellido = "Ana Torres", rut = "111111111",
                departamento = "Adm", descripcionAuto = "-", tipoPersona = "FUNCIONARIO", entradaAt = at(14, 21, 5), salidaAt = at(15, 2, 30))
        )
        vehicles.insert(
            RegistroIngreso(shiftId = shiftId, patente = "TEST10", nombreApellido = "Luis", departamento = "Ops",
                descripcionAuto = "-", tipoPersona = "VISITA", entradaAt = at(14, 22, 0))
        )

        val doc = useCase.shift(shiftId)!!

        assertEquals(ReportDocument.Type.VEHICLE_SHIFT, doc.type)
        assertEquals(
            listOf(
                ReportDocument.Meta(MetaField.POINT, "Punto[porteria_2]"),
                ReportDocument.Meta(MetaField.DATE, "14/06/2026"),
                ReportDocument.Meta(MetaField.GUARD, "Guardia Noche"),
                ReportDocument.Meta(MetaField.SHIFT_TYPE, "Turno[NIGHT]")
            ),
            doc.meta
        )
        assertEquals(
            listOf(Column.INDEX, Column.PATENTE, Column.NOMBRE, Column.RUT, Column.DEPTO, Column.TIPO, Column.ENTRADA, Column.SALIDA),
            doc.columns
        )
        assertEquals(listOf("1", "DEMO01", "Ana Torres", "11.111.111-1", "Adm", "Tipo[FUNCIONARIO]", "21:05", "02:30"), doc.rows[0])
        assertEquals(listOf("2", "TEST10", "Luis", "-", "Ops", "Tipo[VISITA]", "22:00", "--:--"), doc.rows[1])
        assertEquals("reporte_porteria_2_2026-06-14_$shiftId", doc.fileStem)
    }

    @Test
    fun `bike shift report uses bike columns`() = runTest {
        val shiftId = shifts.insert(Turno(guardia = "G", controlPoint = "bicicletero_2", tipo = "DAY", fecha = "2026-06-14", iniciadoAt = at(14, 8, 0)))
        bikes.insert(RegistroBicicleta(shiftId = shiftId, rut = "111111111", nombre = "Ana", carrera = "Ing", tipoPersona = "ESTUDIANTE", entradaAt = at(14, 8, 15)))

        val doc = useCase.shift(shiftId)!!

        assertEquals(ReportDocument.Type.BIKE_SHIFT, doc.type)
        assertEquals(listOf(Column.INDEX, Column.RUT, Column.NOMBRE, Column.TIPO, Column.CARRERA, Column.ENTRADA, Column.SALIDA), doc.columns)
        assertEquals(listOf("1", "11.111.111-1", "Ana", "Tipo[ESTUDIANTE]", "Ing", "08:15", "--:--"), doc.rows.single())
    }

    @Test
    fun `missing shift yields no document`() = runTest {
        assertNull(useCase.shift(404))
    }

    @Test
    fun `daily summary has one row per point and a total`() {
        val doc = useCase.dailySummary(
            listOf(PointStat(ControlPoint.PORTERIA_2, 5, 3), PointStat(ControlPoint.BICICLETERO_1, 2, 2))
        )
        assertEquals(ReportDocument.Type.DAILY_SUMMARY, doc.type)
        assertEquals(listOf(Column.PUNTO, Column.ENTRARON, Column.SALIERON, Column.DENTRO), doc.columns)
        assertEquals(
            listOf(
                listOf("Punto[porteria_2]", "5", "3", "2"),
                listOf("Punto[bicicletero_1]", "2", "2", "0"),
                listOf(ReportDocument.TOTAL_ROW_LABEL, "7", "5", "2")
            ),
            doc.rows
        )
        assertEquals(listOf(ReportDocument.Meta(MetaField.DATE, "15/06/2026")), doc.meta)
    }

    @Test
    fun `directory report lists people`() = runTest {
        personas.upsertAll(listOf(Persona(patente = "DEMO01", nombreApellido = "Ana", departamento = "Adm", descripcionAuto = "-", rut = "111111111", tipoPersona = "VISITA")))
        val doc = useCase.directory()
        assertEquals(listOf(Column.PATENTE, Column.NOMBRE, Column.DEPTO, Column.RUT, Column.TIPO), doc.columns)
        assertEquals(listOf("DEMO01", "Ana", "Adm", "11.111.111-1", "Tipo[VISITA]"), doc.rows.single())
    }
}
