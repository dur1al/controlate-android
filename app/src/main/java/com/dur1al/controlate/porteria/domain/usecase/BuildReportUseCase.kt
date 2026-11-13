package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.PointStat
import com.dur1al.controlate.porteria.domain.report.ReportDocument
import com.dur1al.controlate.porteria.domain.report.ReportDocument.Column
import com.dur1al.controlate.porteria.domain.report.ReportDocument.Meta
import com.dur1al.controlate.porteria.domain.report.ReportDocument.MetaField
import com.dur1al.controlate.porteria.domain.report.ReportLabels
import com.dur1al.controlate.porteria.domain.repository.BikeEntryRepository
import com.dur1al.controlate.porteria.domain.repository.ControlPointRepository
import com.dur1al.controlate.porteria.domain.repository.PersonaRepository
import com.dur1al.controlate.porteria.domain.repository.ShiftRepository
import com.dur1al.controlate.porteria.domain.repository.VehicleEntryRepository
import com.dur1al.controlate.porteria.domain.time.Clock
import com.dur1al.controlate.porteria.domain.time.TimeFormat
import com.dur1al.controlate.porteria.domain.validation.Rut
import java.time.ZoneId

/** Arma el contenido de los reportes (turno, resumen diario, directorio). Lógica pura y testeable. */
class BuildReportUseCase(
    private val shifts: ShiftRepository,
    private val vehicles: VehicleEntryRepository,
    private val bikes: BikeEntryRepository,
    private val personas: PersonaRepository,
    private val points: ControlPointRepository,
    private val labels: ReportLabels,
    private val clock: Clock,
    private val zone: ZoneId = ZoneId.systemDefault()
) {

    /** Reporte de un turno; `null` si el turno no existe. Las filas pueden venir vacías. */
    suspend fun shift(shiftId: Long): ReportDocument? {
        val turno = shifts.get(shiftId) ?: return null
        val point = points.get(turno.controlPoint)
        val kind = point?.kind ?: ControlPoint.Kind.fromId(turno.controlPoint)
        val meta = shiftMeta(turno, point?.name ?: labels.unknownControlPoint(turno.controlPoint))
        return if (kind == ControlPoint.Kind.BICICLETERO) bikeShift(turno, meta) else vehicleShift(turno, meta)
    }

    private suspend fun vehicleShift(turno: Turno, meta: List<Meta>): ReportDocument {
        val rows = vehicles.getByShift(turno.id).mapIndexed { i, r ->
            listOf(
                (i + 1).toString(), r.patente, r.nombreApellido, r.rut.formatRut(), r.departamento,
                labels.personaType(r.tipoPersona), time(r.entradaAt), time(r.salidaAt)
            )
        }
        return ReportDocument(
            type = ReportDocument.Type.VEHICLE_SHIFT,
            meta = meta,
            columns = listOf(Column.INDEX, Column.PATENTE, Column.NOMBRE, Column.RUT, Column.DEPTO, Column.TIPO, Column.ENTRADA, Column.SALIDA),
            rows = rows,
            fileStem = stem("reporte", turno.controlPoint, turno.fecha, turno.id.toString())
        )
    }

    private suspend fun bikeShift(turno: Turno, meta: List<Meta>): ReportDocument {
        val rows = bikes.getByShift(turno.id).mapIndexed { i, r ->
            listOf(
                (i + 1).toString(), r.rut.formatRut(), r.nombre, labels.personaType(r.tipoPersona),
                r.carrera, time(r.entradaAt), time(r.salidaAt)
            )
        }
        return ReportDocument(
            type = ReportDocument.Type.BIKE_SHIFT,
            meta = meta,
            columns = listOf(Column.INDEX, Column.RUT, Column.NOMBRE, Column.TIPO, Column.CARRERA, Column.ENTRADA, Column.SALIDA),
            rows = rows,
            fileStem = stem("reporte", turno.controlPoint, turno.fecha, turno.id.toString())
        )
    }

    /** Resumen administrativo del día actual a partir de estadísticas ya calculadas. */
    fun dailySummary(stats: List<PointStat>): ReportDocument {
        val rows = stats.map {
            listOf(it.controlPoint.name, it.entradas.toString(), it.salidas.toString(), it.dentro.toString())
        }
        val total = listOf(
            ReportDocument.TOTAL_ROW_LABEL,
            stats.sumOf { it.entradas }.toString(),
            stats.sumOf { it.salidas }.toString(),
            stats.sumOf { it.dentro }.toString()
        )
        val now = clock.now()
        return ReportDocument(
            type = ReportDocument.Type.DAILY_SUMMARY,
            meta = listOf(Meta(MetaField.DATE, TimeFormat.displayDate(now, zone))),
            columns = listOf(Column.PUNTO, Column.ENTRARON, Column.SALIERON, Column.DENTRO),
            rows = rows + listOf(total),
            fileStem = stem("resumen", TimeFormat.fileStamp(now, zone))
        )
    }

    suspend fun directory(): ReportDocument {
        val rows = personas.getAll().map {
            listOf(it.patente, it.nombreApellido, it.departamento, it.rut.formatRut(), labels.personaType(it.tipoPersona.orEmpty()))
        }
        return ReportDocument(
            type = ReportDocument.Type.DIRECTORY,
            meta = emptyList(),
            columns = listOf(Column.PATENTE, Column.NOMBRE, Column.DEPTO, Column.RUT, Column.TIPO),
            rows = rows,
            fileStem = stem("directorio", TimeFormat.fileStamp(clock.now(), zone))
        )
    }

    private fun shiftMeta(turno: Turno, pointName: String) = listOf(
        Meta(MetaField.POINT, pointName),
        Meta(MetaField.DATE, TimeFormat.isoToDisplay(turno.fecha)),
        Meta(MetaField.GUARD, turno.guardia),
        Meta(MetaField.SHIFT_TYPE, labels.shiftType(turno.tipo))
    )

    private fun time(epochMs: Long?): String =
        epochMs?.let { TimeFormat.time(it, zone) } ?: ReportDocument.NO_TIME

    private fun String?.formatRut(): String =
        this?.takeIf { it.isNotBlank() }?.let(Rut::format) ?: ReportDocument.EMPTY

    private fun stem(vararg parts: String): String =
        parts.joinToString("_").replace(Regex("[^A-Za-z0-9._-]"), "_")
}
