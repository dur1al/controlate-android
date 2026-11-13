package com.dur1al.controlate.porteria.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import kotlinx.coroutines.flow.Flow

@Dao
interface BicicletaDao {
    @Insert
    suspend fun insert(registro: RegistroBicicleta)

    @Update
    suspend fun update(registro: RegistroBicicleta)

    @Delete
    suspend fun delete(registro: RegistroBicicleta)

    @Query("SELECT * FROM registros_bicicleta WHERE estado = 'ENTRADA' ORDER BY id DESC")
    fun getRegistrosActivos(): Flow<List<RegistroBicicleta>>

    @Query("SELECT * FROM registros_bicicleta ORDER BY id DESC")
    fun getAllRegistros(): Flow<List<RegistroBicicleta>>

    @Query("SELECT * FROM registros_bicicleta WHERE shiftId = :shiftId ORDER BY id DESC")
    fun getRegistrosByShift(shiftId: Long): Flow<List<RegistroBicicleta>>

    @Query("SELECT * FROM registros_bicicleta WHERE shiftId = :shiftId ORDER BY id ASC")
    suspend fun getRecordsByShiftList(shiftId: Long): List<RegistroBicicleta>

    @Query("SELECT shiftId, fecha, controlPoint, MIN(horaEntrada) as horaInicio, MAX(horaSalida) as horaFin, COUNT(*) as totalRegistros, COALESCE(SUM(CASE WHEN horaSalida IS NOT NULL THEN 1 ELSE 0 END), 0) as totalSalidas, MAX(guardiaTurno) as guardiaTurno, MAX(turnoTipo) as turnoTipo FROM registros_bicicleta GROUP BY shiftId ORDER BY shiftId DESC")
    fun getAllShiftSummaries(): Flow<List<BicicletaShiftSummary>>

    @Query("SELECT * FROM registros_bicicleta WHERE rut = :rut ORDER BY id DESC LIMIT 1")
    suspend fun getUltimoRegistroPorRut(rut: String): RegistroBicicleta?

    @Query("SELECT COUNT(*) FROM registros_bicicleta WHERE fecha = :fecha")
    suspend fun countRegistrosByFecha(fecha: String): Int

    @Query("SELECT COUNT(*) FROM registros_bicicleta WHERE fecha = :fecha AND horaSalida IS NOT NULL")
    suspend fun countSalidasByFecha(fecha: String): Int

    @Query("SELECT COUNT(*) FROM registros_bicicleta WHERE fecha = :fecha AND controlPoint = :controlPoint")
    suspend fun countRegistrosByFechaAndPoint(fecha: String, controlPoint: String): Int

    @Query("SELECT COUNT(*) FROM registros_bicicleta WHERE fecha = :fecha AND controlPoint = :controlPoint AND horaSalida IS NOT NULL")
    suspend fun countSalidasByFechaAndPoint(fecha: String, controlPoint: String): Int

    @Query("SELECT * FROM registros_bicicleta WHERE fecha = :fecha ORDER BY id ASC")
    suspend fun getRecordsByDateList(fecha: String): List<RegistroBicicleta>

    @Query("SELECT * FROM registros_bicicleta WHERE fecha = :fecha AND controlPoint = :controlPoint ORDER BY id ASC")
    suspend fun getRecordsByDateAndPointList(fecha: String, controlPoint: String): List<RegistroBicicleta>

    @Query("DELETE FROM registros_bicicleta WHERE shiftId = :shiftId")
    suspend fun deleteByShift(shiftId: Long)

    @Query("DELETE FROM registros_bicicleta")
    suspend fun deleteAll()

    data class BicicletaShiftSummary(
        val shiftId: Long,
        val fecha: String,
        val controlPoint: String,
        val horaInicio: String,
        val horaFin: String?,
        val totalRegistros: Int,
        val totalSalidas: Int,
        val guardiaTurno: String,
        val turnoTipo: String
    )
}
