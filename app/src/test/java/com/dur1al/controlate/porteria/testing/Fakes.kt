package com.dur1al.controlate.porteria.testing

import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.PointCount
import com.dur1al.controlate.porteria.domain.model.ShiftSummary
import com.dur1al.controlate.porteria.domain.repository.BikeEntryRepository
import com.dur1al.controlate.porteria.domain.repository.ControlPointRepository
import com.dur1al.controlate.porteria.domain.repository.PersonaRepository
import com.dur1al.controlate.porteria.domain.repository.SessionStore
import com.dur1al.controlate.porteria.domain.repository.ShiftRepository
import com.dur1al.controlate.porteria.domain.repository.VehicleEntryRepository
import com.dur1al.controlate.porteria.domain.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Reloj controlable desde el test. */
class FakeClock(var nowMs: Long = 1_750_000_000_000L) : Clock {
    override fun now(): Long = nowMs
    fun advanceMinutes(minutes: Long) { nowMs += minutes * 60_000 }
}

class FakeSessionStore(private var id: Long? = null) : SessionStore {
    override fun activeShiftId(): Long? = id
    override fun setActiveShift(id: Long) { this.id = id }
    override fun clear() { id = null }
}

class FakeShiftRepository(
    private val vehicles: FakeVehicleEntryRepository? = null,
    private val bikes: FakeBikeEntryRepository? = null
) : ShiftRepository {
    val turnos = MutableStateFlow<Map<Long, Turno>>(emptyMap())
    var pointCounts: List<PointCount> = emptyList()
    var lastCountRange: LongRange? = null
    private var nextId = 1L

    override suspend fun insert(turno: Turno): Long {
        val id = if (turno.id == 0L) nextId++ else turno.id
        turnos.value = turnos.value + (id to turno.copy(id = id))
        return id
    }

    override suspend fun get(id: Long): Turno? = turnos.value[id]

    override suspend fun finish(id: Long, at: Long): Boolean {
        val t = turnos.value[id] ?: return false
        if (t.finalizadoAt != null) return false
        turnos.value = turnos.value + (id to t.copy(finalizadoAt = at))
        return true
    }

    override suspend fun delete(id: Long) {
        turnos.value = turnos.value - id
        vehicles?.removeShift(id)
        bikes?.removeShift(id)
    }

    override suspend fun deleteFinished() {
        val finished = turnos.value.values.filter { it.finalizadoAt != null }.map { it.id }
        finished.forEach { delete(it) }
    }

    override fun observeSummaries(): Flow<List<ShiftSummary>> = turnos.map { map ->
        map.values.sortedByDescending { it.iniciadoAt }.map { t ->
            val v = vehicles?.rows?.value.orEmpty().filter { it.shiftId == t.id }
            val b = bikes?.rows?.value.orEmpty().filter { it.shiftId == t.id }
            ShiftSummary(
                t.id, t.guardia, t.controlPoint, t.tipo, t.fecha, t.iniciadoAt, t.finalizadoAt,
                v.size + b.size, v.count { it.salidaAt != null } + b.count { it.salidaAt != null }
            )
        }
    }

    override suspend fun countByPoint(from: Long, to: Long): List<PointCount> {
        lastCountRange = from..to
        return pointCounts
    }
}

class FakeVehicleEntryRepository : VehicleEntryRepository {
    val rows = MutableStateFlow<List<RegistroIngreso>>(emptyList())
    private var nextId = 1L

    /** Simula un fallo de la base al insertar. */
    var failInsert = false

    override fun observeByShift(shiftId: Long, query: String): Flow<List<RegistroIngreso>> = rows.map { list ->
        list.filter {
            it.shiftId == shiftId && (query.isEmpty() ||
                it.patente.contains(query, true) || it.nombreApellido.contains(query, true))
        }.sortedByDescending { it.entradaAt }
    }

    override suspend fun insert(registro: RegistroIngreso): Long {
        check(!failInsert) { "disk I/O error" }
        val id = nextId++
        rows.value = rows.value + registro.copy(id = id)
        return id
    }

    override suspend fun getByShift(shiftId: Long) = rows.value.filter { it.shiftId == shiftId }
    override suspend fun getBetween(from: Long, to: Long) = rows.value.filter { it.entradaAt in from..to }
    override suspend fun isInside(shiftId: Long, patente: String) =
        rows.value.any { it.shiftId == shiftId && it.patente == patente && it.salidaAt == null }

    override suspend fun markExit(id: Long, at: Long): Boolean = update(id) { if (it.salidaAt == null) it.copy(salidaAt = at) else null }
    override suspend fun undoExit(id: Long, at: Long): Boolean = update(id) { if (it.salidaAt == at) it.copy(salidaAt = null) else null }
    override suspend fun deleteAll() { rows.value = emptyList() }

    fun removeShift(shiftId: Long) { rows.value = rows.value.filterNot { it.shiftId == shiftId } }

    private fun update(id: Long, change: (RegistroIngreso) -> RegistroIngreso?): Boolean {
        val current = rows.value.firstOrNull { it.id == id } ?: return false
        val updated = change(current) ?: return false
        rows.value = rows.value.map { if (it.id == id) updated else it }
        return true
    }
}

class FakeBikeEntryRepository : BikeEntryRepository {
    val rows = MutableStateFlow<List<RegistroBicicleta>>(emptyList())
    private var nextId = 1L

    override fun observeByShift(shiftId: Long, query: String): Flow<List<RegistroBicicleta>> = rows.map { list ->
        list.filter {
            it.shiftId == shiftId && (query.isEmpty() || it.rut.contains(query) || it.nombre.contains(query, true))
        }.sortedByDescending { it.entradaAt }
    }

    override suspend fun insert(registro: RegistroBicicleta): Long {
        val id = nextId++
        rows.value = rows.value + registro.copy(id = id)
        return id
    }

    override suspend fun getByShift(shiftId: Long) = rows.value.filter { it.shiftId == shiftId }
    override suspend fun getBetween(from: Long, to: Long) = rows.value.filter { it.entradaAt in from..to }
    override suspend fun latestByRut(rut: String) = rows.value.filter { it.rut == rut }.maxByOrNull { it.entradaAt }
    override suspend fun isInside(shiftId: Long, rut: String) =
        rows.value.any { it.shiftId == shiftId && it.rut == rut && it.salidaAt == null }

    override suspend fun markExit(id: Long, at: Long): Boolean = update(id) { if (it.salidaAt == null) it.copy(salidaAt = at) else null }
    override suspend fun undoExit(id: Long, at: Long): Boolean = update(id) { if (it.salidaAt == at) it.copy(salidaAt = null) else null }
    override suspend fun deleteAll() { rows.value = emptyList() }

    fun removeShift(shiftId: Long) { rows.value = rows.value.filterNot { it.shiftId == shiftId } }

    private fun update(id: Long, change: (RegistroBicicleta) -> RegistroBicicleta?): Boolean {
        val current = rows.value.firstOrNull { it.id == id } ?: return false
        val updated = change(current) ?: return false
        rows.value = rows.value.map { if (it.id == id) updated else it }
        return true
    }
}

class FakePersonaRepository(initial: List<Persona> = emptyList()) : PersonaRepository {
    val rows = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun search(query: String): Flow<List<Persona>> = rows.map { list ->
        list.filter { query.isEmpty() || it.patente.contains(query, true) || it.nombreApellido.contains(query, true) }
    }
    override suspend fun getAll() = rows.value
    override suspend fun getByPatente(patente: String) = rows.value.firstOrNull { it.patente == patente }
    override suspend fun getByRut(rut: String) = rows.value.firstOrNull { it.rut == rut }

    override suspend fun insertIfAbsent(persona: Persona) {
        if (rows.value.any { it.patente == persona.patente }) return
        rows.value = rows.value + persona.copy(id = nextId++)
    }

    override suspend fun upsertAll(personas: List<Persona>) {
        personas.forEach { p ->
            rows.value = rows.value.filterNot { it.patente == p.patente } + p.copy(id = nextId++)
        }
    }

    override suspend fun update(persona: Persona) {
        // Emula el índice único de Room sobre `patente`.
        check(rows.value.none { it.id != persona.id && it.patente == persona.patente }) { "UNIQUE constraint failed: personas.patente" }
        rows.value = rows.value.map { if (it.id == persona.id) persona else it }
    }
    override suspend fun delete(persona: Persona) { rows.value = rows.value.filterNot { it.id == persona.id } }
}

/** Repositorio de puntos en memoria; arranca con los puntos de fábrica, igual que una instalación nueva. */
class FakeControlPointRepository(initial: List<ControlPoint> = ControlPoint.DEFAULTS) : ControlPointRepository {
    val rows = MutableStateFlow(initial)

    /** Simula un fallo de la base al escribir. */
    var failWrites = false

    // Mismo orden que `PuntoControlDao`: porterías primero y luego por `orden`.
    private fun List<ControlPoint>.sorted() = sortedWith(compareBy({ it.kind != ControlPoint.Kind.PORTERIA }, { it.order }))

    override fun observeAll(): Flow<List<ControlPoint>> = rows.map { it.sorted() }
    override suspend fun getAll() = rows.value.sorted()
    override suspend fun get(id: String) = rows.value.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }

    override suspend fun insert(point: ControlPoint) {
        check(!failWrites) { "disk I/O error" }
        check(rows.value.none { it.id == point.id }) { "UNIQUE constraint failed: puntos_control.id" }
        rows.value = rows.value + point
    }

    override suspend fun update(point: ControlPoint) {
        check(!failWrites) { "disk I/O error" }
        rows.value = rows.value.map { if (it.id == point.id) point else it }
    }
}

/** Atajos a los puntos de fábrica para los tests (antes eran constantes del enum). */
object Points {
    val PORTERIA_1 = ControlPoint.DEFAULTS[0]
    val PORTERIA_2 = ControlPoint.DEFAULTS[1]
    val PORTERIA_3 = ControlPoint.DEFAULTS[2]
    val BICICLETERO_1 = ControlPoint.DEFAULTS[3]
    val BICICLETERO_2 = ControlPoint.DEFAULTS[4]
}
