package com.dur1al.controlate.porteria.data.local.dao

import androidx.room.*
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(registro: RegistroIngreso)

    @Update
    suspend fun update(registro: RegistroIngreso)

    @Delete
    suspend fun delete(registro: RegistroIngreso)

    @Query("SELECT * FROM registros_ingreso WHERE shiftId = :shiftId ORDER BY id DESC")
    fun getRegistrosByShift(shiftId: Long): Flow<List<RegistroIngreso>>

    @Query("SELECT * FROM registros_ingreso WHERE (patente LIKE '%' || :query || '%' OR nombreApellido LIKE '%' || :query || '%') AND shiftId = :shiftId ORDER BY id DESC")
    fun searchRegistrosByShift(query: String, shiftId: Long): Flow<List<RegistroIngreso>>

    @Query("SELECT * FROM registros_ingreso WHERE fechaEntrada = :fecha ORDER BY id DESC")
    fun getRegistrosByFecha(fecha: String): Flow<List<RegistroIngreso>>

    @Query("SELECT * FROM registros_ingreso ORDER BY id ASC")
    suspend fun getAllRecordsList(): List<RegistroIngreso>

    @Query("SELECT * FROM registros_ingreso WHERE fechaEntrada = :date ORDER BY id ASC")
    suspend fun getRecordsByDateList(date: String): List<RegistroIngreso>
    
    @Query("SELECT * FROM registros_ingreso WHERE shiftId = :shiftId ORDER BY id ASC")
    suspend fun getRecordsByShiftList(shiftId: Long): List<RegistroIngreso>

    @Query("DELETE FROM registros_ingreso")
    suspend fun deleteAll()

    @Query("DELETE FROM registros_ingreso WHERE shiftId = :shiftId")
    suspend fun deleteByShift(shiftId: Long)
    
    @Query("SELECT COUNT(*) FROM registros_ingreso WHERE fechaEntrada = :fecha")
    suspend fun countRegistrosByFecha(fecha: String): Int

    @Query("SELECT COUNT(*) FROM registros_ingreso WHERE fechaEntrada = :fecha AND horaSalida IS NOT NULL")
    suspend fun countSalidasByFecha(fecha: String): Int

    @Query("SELECT COUNT(*) FROM registros_ingreso WHERE fechaEntrada = :fecha AND porteria = :porteria")
    suspend fun countRegistrosByFechaAndPoint(fecha: String, porteria: String): Int

    @Query("SELECT COUNT(*) FROM registros_ingreso WHERE fechaEntrada = :fecha AND porteria = :porteria AND horaSalida IS NOT NULL")
    suspend fun countSalidasByFechaAndPoint(fecha: String, porteria: String): Int

    @Query("SELECT shiftId, guardiaTurno, fechaTurno, porteria, turnoTipo, MIN(horaEntrada) as horaInicio, MAX(horaSalida) as horaFin, COUNT(*) as totalRegistros, COALESCE(SUM(CASE WHEN horaSalida IS NOT NULL THEN 1 ELSE 0 END), 0) as totalSalidas FROM registros_ingreso GROUP BY shiftId ORDER BY shiftId DESC")
    fun getAllShifts(): Flow<List<ShiftSummary>>

    data class ShiftSummary(
        val shiftId: Long,
        val guardiaTurno: String,
        val fechaTurno: String,
        val porteria: String,
        val turnoTipo: String,
        val horaInicio: String,
        val horaFin: String?,
        val totalRegistros: Int,
        val totalSalidas: Int
    )
}
