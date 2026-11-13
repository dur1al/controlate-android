package com.dur1al.controlate.porteria.data.local.dao

import androidx.room.*
import com.dur1al.controlate.porteria.data.local.entities.Persona
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(persona: Persona)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(personas: List<Persona>)

    @Update
    suspend fun update(persona: Persona)

    @Delete
    suspend fun delete(persona: Persona)

    @Query("SELECT * FROM personas")
    fun getAllPersonas(): Flow<List<Persona>>

    @Query("SELECT COUNT(*) FROM personas")
    suspend fun getCount(): Int

    @Query("SELECT * FROM personas WHERE patente = :patente LIMIT 1")
    suspend fun getPersonaByPatente(patente: String): Persona?

    @Query("SELECT * FROM personas WHERE rut = :rut LIMIT 1")
    suspend fun getPersonaByRut(rut: String): Persona?

    @Query("SELECT * FROM personas WHERE (patente LIKE '%' || :query || '%' OR nombreApellido LIKE '%' || :query || '%' OR rut LIKE '%' || :query || '%') ORDER BY nombreApellido ASC")
    fun searchPersonas(query: String): Flow<List<Persona>>

    @Query("DELETE FROM personas")
    suspend fun deleteAll()
}
