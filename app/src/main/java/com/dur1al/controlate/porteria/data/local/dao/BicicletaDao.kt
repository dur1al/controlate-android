package com.dur1al.controlate.porteria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import kotlinx.coroutines.flow.Flow

@Dao
interface BicicletaDao {

    @Insert
    suspend fun insert(registro: RegistroBicicleta): Long

    @Query(
        """
        SELECT * FROM registros_bicicleta
        WHERE shiftId = :shiftId
          AND (:query = '' OR instr(rut, upper(:query)) > 0 OR instr(upper(nombre), upper(:query)) > 0)
        ORDER BY entradaAt DESC, id DESC
        """
    )
    fun observeByShift(shiftId: Long, query: String): Flow<List<RegistroBicicleta>>

    @Query("SELECT * FROM registros_bicicleta WHERE shiftId = :shiftId ORDER BY entradaAt ASC, id ASC")
    suspend fun getByShift(shiftId: Long): List<RegistroBicicleta>

    @Query("SELECT * FROM registros_bicicleta WHERE entradaAt BETWEEN :from AND :to ORDER BY entradaAt ASC, id ASC")
    suspend fun getBetween(from: Long, to: Long): List<RegistroBicicleta>

    @Query("SELECT * FROM registros_bicicleta WHERE rut = :rut ORDER BY entradaAt DESC LIMIT 1")
    suspend fun getLatestByRut(rut: String): RegistroBicicleta?

    /** ¿El RUT ya tiene una bicicleta sin salida en este turno? (evita ingresos duplicados). */
    @Query("SELECT EXISTS(SELECT 1 FROM registros_bicicleta WHERE shiftId = :shiftId AND rut = :rut AND salidaAt IS NULL)")
    suspend fun isInside(shiftId: Long, rut: String): Boolean

    @Query("UPDATE registros_bicicleta SET salidaAt = :at WHERE id = :id AND salidaAt IS NULL")
    suspend fun markExit(id: Long, at: Long): Int

    @Query("UPDATE registros_bicicleta SET salidaAt = NULL WHERE id = :id AND salidaAt = :at")
    suspend fun undoExit(id: Long, at: Long): Int

    @Query("DELETE FROM registros_bicicleta")
    suspend fun deleteAll()
}
