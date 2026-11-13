package com.dur1al.controlate.porteria.data.repository

import com.dur1al.controlate.porteria.data.local.dao.BicicletaDao
import com.dur1al.controlate.porteria.data.local.dao.PersonaDao
import com.dur1al.controlate.porteria.data.local.dao.PuntoControlDao
import com.dur1al.controlate.porteria.data.local.dao.RegistroDao
import com.dur1al.controlate.porteria.data.local.dao.TurnoDao
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.PuntoControl
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.repository.BikeEntryRepository
import com.dur1al.controlate.porteria.domain.repository.ControlPointRepository
import com.dur1al.controlate.porteria.domain.repository.PersonaRepository
import com.dur1al.controlate.porteria.domain.repository.ShiftRepository
import com.dur1al.controlate.porteria.domain.repository.VehicleEntryRepository
import kotlinx.coroutines.flow.map

/*
 * Implementaciones Room de los contratos de dominio. Room ejecuta las funciones `suspend`
 * en su propio executor de I/O, por lo que son seguras de llamar desde el hilo principal.
 */

class RoomShiftRepository(private val dao: TurnoDao) : ShiftRepository {
    override suspend fun insert(turno: Turno) = dao.insert(turno)
    override suspend fun get(id: Long) = dao.getById(id)
    override suspend fun finish(id: Long, at: Long) = dao.finish(id, at) > 0
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun deleteFinished() = dao.deleteFinished()
    override fun observeSummaries() = dao.observeSummaries()
    override suspend fun countByPoint(from: Long, to: Long) = dao.countByPoint(from, to)
}

class RoomVehicleEntryRepository(private val dao: RegistroDao) : VehicleEntryRepository {
    override fun observeByShift(shiftId: Long, query: String) = dao.observeByShift(shiftId, query.trim())
    override suspend fun insert(registro: RegistroIngreso) = dao.insert(registro)
    override suspend fun getByShift(shiftId: Long) = dao.getByShift(shiftId)
    override suspend fun getBetween(from: Long, to: Long) = dao.getBetween(from, to)
    override suspend fun isInside(shiftId: Long, patente: String) = dao.isInside(shiftId, patente)
    override suspend fun markExit(id: Long, at: Long) = dao.markExit(id, at) > 0
    override suspend fun undoExit(id: Long, at: Long) = dao.undoExit(id, at) > 0
    override suspend fun deleteAll() = dao.deleteAll()
}

class RoomBikeEntryRepository(private val dao: BicicletaDao) : BikeEntryRepository {
    override fun observeByShift(shiftId: Long, query: String) = dao.observeByShift(shiftId, query.trim())
    override suspend fun insert(registro: RegistroBicicleta) = dao.insert(registro)
    override suspend fun getByShift(shiftId: Long) = dao.getByShift(shiftId)
    override suspend fun getBetween(from: Long, to: Long) = dao.getBetween(from, to)
    override suspend fun latestByRut(rut: String) = dao.getLatestByRut(rut)
    override suspend fun isInside(shiftId: Long, rut: String) = dao.isInside(shiftId, rut)
    override suspend fun markExit(id: Long, at: Long) = dao.markExit(id, at) > 0
    override suspend fun undoExit(id: Long, at: Long) = dao.undoExit(id, at) > 0
    override suspend fun deleteAll() = dao.deleteAll()
}

class RoomControlPointRepository(private val dao: PuntoControlDao) : ControlPointRepository {
    override fun observeAll() = dao.observeAll().map { rows -> rows.mapNotNull { it.toDomain() } }
    override suspend fun getAll() = dao.getAll().mapNotNull { it.toDomain() }
    override suspend fun get(id: String) = dao.getById(id.trim())?.toDomain()
    override suspend fun insert(point: ControlPoint) = dao.insert(point.toEntity())
    override suspend fun update(point: ControlPoint) = dao.update(point.toEntity())

    /** Una fila con tipo desconocido (dato corrupto) se omite en vez de romper la pantalla. */
    private fun PuntoControl.toDomain() =
        ControlPoint.Kind.fromCode(tipo)?.let { ControlPoint(id, nombre, it, orden, activo) }

    private fun ControlPoint.toEntity() = PuntoControl(id, name, kind.name, order, active)
}

class RoomPersonaRepository(private val dao: PersonaDao) : PersonaRepository {
    override fun search(query: String) = dao.search(query.trim())
    override suspend fun getAll() = dao.getAll()
    override suspend fun getByPatente(patente: String) = dao.getByPatente(patente)
    override suspend fun getByRut(rut: String) = dao.getByRut(rut)
    override suspend fun insertIfAbsent(persona: Persona) { dao.insertIfAbsent(persona) }
    override suspend fun upsertAll(personas: List<Persona>) = dao.upsertAll(personas)
    override suspend fun update(persona: Persona) = dao.update(persona)
    override suspend fun delete(persona: Persona) = dao.delete(persona)
}
