package com.dur1al.controlate.porteria.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dur1al.controlate.porteria.data.local.entities.Persona
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {

    /** Alta automática desde un registro: nunca sobrescribe a una persona existente. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(persona: Persona): Long

    /** Importación masiva: la patente es la clave natural; la fila importada reemplaza a la existente. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(personas: List<Persona>)

    @Update
    suspend fun update(persona: Persona)

    @Delete
    suspend fun delete(persona: Persona)

    @Query("SELECT * FROM personas WHERE patente = :patente LIMIT 1")
    suspend fun getByPatente(patente: String): Persona?

    @Query("SELECT * FROM personas WHERE rut = :rut LIMIT 1")
    suspend fun getByRut(rut: String): Persona?

    @Query(
        """
        SELECT * FROM personas
        WHERE :query = ''
           OR instr(upper(patente), upper(:query)) > 0
           OR instr(upper(nombreApellido), upper(:query)) > 0
           OR instr(coalesce(rut, ''), upper(:query)) > 0
        ORDER BY nombreApellido COLLATE NOCASE ASC
        """
    )
    fun search(query: String): Flow<List<Persona>>

    @Query("SELECT * FROM personas ORDER BY nombreApellido COLLATE NOCASE ASC")
    suspend fun getAll(): List<Persona>

    @Query("SELECT COUNT(*) FROM personas")
    suspend fun count(): Int
}
