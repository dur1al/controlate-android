package com.dur1al.controlate.porteria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDao {

    @Insert
    suspend fun insert(registro: RegistroIngreso): Long

    @Query("SELECT * FROM registros_ingreso WHERE id = :id")
    suspend fun getById(id: Long): RegistroIngreso?

    /**
     * Registros del turno; [query] filtra por patente o nombre. Se usa `instr` en lugar de
     * `LIKE` para que `%` o `_` escritos por el usuario no actúen como comodines.
     */
    @Query(
        """
        SELECT * FROM registros_ingreso
        WHERE shiftId = :shiftId
          AND (:query = '' OR instr(upper(patente), upper(:query)) > 0 OR instr(upper(nombreApellido), upper(:query)) > 0)
        ORDER BY entradaAt DESC, id DESC
        """
    )
    fun observeByShift(shiftId: Long, query: String): Flow<List<RegistroIngreso>>

    @Query("SELECT * FROM registros_ingreso WHERE shiftId = :shiftId ORDER BY entradaAt ASC, id ASC")
    suspend fun getByShift(shiftId: Long): List<RegistroIngreso>

    @Query("SELECT * FROM registros_ingreso WHERE entradaAt BETWEEN :from AND :to ORDER BY entradaAt ASC, id ASC")
    suspend fun getBetween(from: Long, to: Long): List<RegistroIngreso>

    /** Marca la salida solo si no existía; devuelve filas afectadas (0 = ya tenía salida). */
    @Query("UPDATE registros_ingreso SET salidaAt = :at WHERE id = :id AND salidaAt IS NULL")
    suspend fun markExit(id: Long, at: Long): Int

    /** Revierte una salida concreta (deshacer) sin pisar otra salida distinta. */
    @Query("UPDATE registros_ingreso SET salidaAt = NULL WHERE id = :id AND salidaAt = :at")
    suspend fun undoExit(id: Long, at: Long): Int

    @Query("DELETE FROM registros_ingreso")
    suspend fun deleteAll()
}
