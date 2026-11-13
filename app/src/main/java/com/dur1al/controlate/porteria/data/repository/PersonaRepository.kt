package com.dur1al.controlate.porteria.data.repository

import com.dur1al.controlate.porteria.data.local.dao.PersonaDao
import com.dur1al.controlate.porteria.data.local.entities.Persona
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio dedicado a operaciones sobre el directorio de personas.
 * Separado del RegistroRepository original para Single Responsibility.
 */
class PersonaRepository(private val personaDao: PersonaDao) {

    val allPersonas: Flow<List<Persona>> = personaDao.getAllPersonas()

    fun searchPersonas(query: String): Flow<List<Persona>> =
        personaDao.searchPersonas(query)

    suspend fun getPersonaByPatente(patente: String): Persona? =
        personaDao.getPersonaByPatente(patente.uppercase().replace(" ", ""))

    suspend fun getPersonaByRut(rut: String): Persona? =
        personaDao.getPersonaByRut(rut.replace(".", "").replace("-", ""))

    suspend fun insert(persona: Persona) = personaDao.insert(persona)

    suspend fun update(persona: Persona) = personaDao.update(persona)

    suspend fun delete(persona: Persona) = personaDao.delete(persona)

    suspend fun insertAll(personas: List<Persona>) = personaDao.insertAll(personas)

    suspend fun getCount(): Int = personaDao.getCount()
}
