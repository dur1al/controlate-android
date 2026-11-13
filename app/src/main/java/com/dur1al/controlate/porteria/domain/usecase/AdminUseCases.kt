package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.domain.importer.PersonaCsvParser
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.PointStat
import com.dur1al.controlate.porteria.domain.repository.BikeEntryRepository
import com.dur1al.controlate.porteria.domain.repository.PersonaRepository
import com.dur1al.controlate.porteria.domain.repository.ShiftRepository
import com.dur1al.controlate.porteria.domain.repository.VehicleEntryRepository
import com.dur1al.controlate.porteria.domain.time.Clock
import com.dur1al.controlate.porteria.domain.time.TimeFormat
import java.time.ZoneId

/** Entradas/salidas del día local en curso, una fila por punto de control (incluye ceros). */
class GetDailyStatsUseCase(
    private val shifts: ShiftRepository,
    private val clock: Clock,
    private val zone: ZoneId = ZoneId.systemDefault()
) {
    suspend operator fun invoke(): List<PointStat> {
        val range = TimeFormat.dayRange(TimeFormat.today(clock.now(), zone), zone)
        val counts = shifts.countByPoint(range.first, range.last)
            .mapNotNull { c -> ControlPoint.fromId(c.controlPoint)?.let { it to c } }
            .toMap()
        return ControlPoint.entries.map { point ->
            val c = counts[point]
            PointStat(point, c?.entradas ?: 0, c?.salidas ?: 0)
        }
    }
}

/**
 * Borra los datos operativos (registros de portería y bicicletero y turnos cerrados).
 * El directorio de personas y el turno abierto se conservan.
 */
class ClearOperationalDataUseCase(
    private val shifts: ShiftRepository,
    private val vehicles: VehicleEntryRepository,
    private val bikes: BikeEntryRepository
) {
    suspend operator fun invoke() {
        vehicles.deleteAll()
        bikes.deleteAll()
        shifts.deleteFinished()
    }
}

/** Importación masiva del directorio: todo o nada, para no dejar archivos a medio cargar. */
class ImportPersonasUseCase(private val personas: PersonaRepository) {
    suspend operator fun invoke(content: String): PersonaCsvParser.Result {
        val result = PersonaCsvParser.parse(content)
        if (result.errors.isEmpty() && result.personas.isNotEmpty()) {
            personas.upsertAll(result.personas)
        }
        return result
    }
}
