package com.dur1al.controlate.porteria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dur1al.controlate.porteria.data.local.entities.PuntoControl
import kotlinx.coroutines.flow.Flow

@Dao
interface PuntoControlDao {

    @Query("SELECT * FROM puntos_control ORDER BY tipo DESC, orden ASC")
    fun observeAll(): Flow<List<PuntoControl>>

    @Query("SELECT * FROM puntos_control ORDER BY tipo DESC, orden ASC")
    suspend fun getAll(): List<PuntoControl>

    /** Sin distinguir mayúsculas: hay turnos heredados con ids en mayúscula (`BICICLETERO_2`). */
    @Query("SELECT * FROM puntos_control WHERE id = :id COLLATE NOCASE LIMIT 1")
    suspend fun getById(id: String): PuntoControl?

    /** Falla si el id ya existe: un id nunca se reutiliza. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(punto: PuntoControl)

    @Update
    suspend fun update(punto: PuntoControl)
}
