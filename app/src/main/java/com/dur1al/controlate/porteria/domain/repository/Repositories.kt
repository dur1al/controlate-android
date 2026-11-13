package com.dur1al.controlate.porteria.domain.repository

import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.PointCount
import com.dur1al.controlate.porteria.domain.model.ShiftSummary
import kotlinx.coroutines.flow.Flow

/*
 * Contratos que la capa de dominio necesita de la persistencia. Las implementaciones viven en
 * `data/repository` (Room); los tests usan fakes en memoria. Las entidades Room se reutilizan
 * como modelos de dominio para evitar mapeos 1:1 sin valor (ver ADR-005 en docs/ARCHITECTURE.md).
 */

/** Puntero al turno abierto en este dispositivo. */
interface SessionStore {
    fun activeShiftId(): Long?
    fun setActiveShift(id: Long)
    fun clear()
}

interface ShiftRepository {
    suspend fun insert(turno: Turno): Long
    suspend fun get(id: Long): Turno?
    /** @return `true` si el turno estaba abierto y quedó cerrado. */
    suspend fun finish(id: Long, at: Long): Boolean
    suspend fun delete(id: Long)
    suspend fun deleteFinished()
    fun observeSummaries(): Flow<List<ShiftSummary>>
    suspend fun countByPoint(from: Long, to: Long): List<PointCount>
}

interface VehicleEntryRepository {
    fun observeByShift(shiftId: Long, query: String): Flow<List<RegistroIngreso>>
    suspend fun insert(registro: RegistroIngreso): Long
    suspend fun getByShift(shiftId: Long): List<RegistroIngreso>
    suspend fun getBetween(from: Long, to: Long): List<RegistroIngreso>
    /** `true` si [patente] (normalizada) tiene un ingreso sin salida en el turno. */
    suspend fun isInside(shiftId: Long, patente: String): Boolean
    /** @return `true` si se marcó; `false` si ya tenía salida o no existe. */
    suspend fun markExit(id: Long, at: Long): Boolean
    suspend fun undoExit(id: Long, at: Long): Boolean
    suspend fun deleteAll()
}

interface BikeEntryRepository {
    fun observeByShift(shiftId: Long, query: String): Flow<List<RegistroBicicleta>>
    suspend fun insert(registro: RegistroBicicleta): Long
    suspend fun getByShift(shiftId: Long): List<RegistroBicicleta>
    suspend fun getBetween(from: Long, to: Long): List<RegistroBicicleta>
    suspend fun latestByRut(rut: String): RegistroBicicleta?
    /** `true` si [rut] (normalizado) tiene una bicicleta sin salida en el turno. */
    suspend fun isInside(shiftId: Long, rut: String): Boolean
    suspend fun markExit(id: Long, at: Long): Boolean
    suspend fun undoExit(id: Long, at: Long): Boolean
    suspend fun deleteAll()
}

/** Puntos de control configurables (porterías y bicicleteros), activos e inactivos. */
interface ControlPointRepository {
    fun observeAll(): Flow<List<ControlPoint>>
    suspend fun getAll(): List<ControlPoint>
    suspend fun get(id: String): ControlPoint?
    suspend fun insert(point: ControlPoint)
    suspend fun update(point: ControlPoint)
}

interface PersonaRepository {
    fun search(query: String): Flow<List<Persona>>
    suspend fun getAll(): List<Persona>
    suspend fun getByPatente(patente: String): Persona?
    suspend fun getByRut(rut: String): Persona?
    suspend fun insertIfAbsent(persona: Persona)
    suspend fun upsertAll(personas: List<Persona>)
    suspend fun update(persona: Persona)
    suspend fun delete(persona: Persona)
}
