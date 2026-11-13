package com.dur1al.controlate.porteria.domain.repository

import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.data.local.entities.ExamenResultado
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.data.local.entities.Turno
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
    suspend fun markExit(id: Long, at: Long): Boolean
    suspend fun undoExit(id: Long, at: Long): Boolean
    suspend fun deleteAll()
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

interface ExamRepository {
    suspend fun randomQuestions(limit: Int): List<ExamenPregunta>
    suspend fun saveResult(resultado: ExamenResultado): Long
}
