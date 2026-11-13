package com.dur1al.controlate.porteria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.model.PointCount
import com.dur1al.controlate.porteria.domain.model.ShiftSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface TurnoDao {

    @Insert
    suspend fun insert(turno: Turno): Long

    @Query("SELECT * FROM turnos WHERE id = :id")
    suspend fun getById(id: Long): Turno?

    @Query("UPDATE turnos SET finalizadoAt = :at WHERE id = :id AND finalizadoAt IS NULL")
    suspend fun finish(id: Long, at: Long): Int

    @Query("DELETE FROM turnos WHERE id = :id")
    suspend fun delete(id: Long)

    /** Borra los turnos cerrados; sus registros se eliminan en cascada (FK `ON DELETE CASCADE`). */
    @Query("DELETE FROM turnos WHERE finalizadoAt IS NOT NULL")
    suspend fun deleteFinished()

    @Query(
        """
        SELECT t.id AS shiftId, t.guardia, t.controlPoint, t.tipo, t.fecha, t.iniciadoAt, t.finalizadoAt,
            (SELECT COUNT(*) FROM registros_ingreso r WHERE r.shiftId = t.id)
              + (SELECT COUNT(*) FROM registros_bicicleta b WHERE b.shiftId = t.id) AS totalRegistros,
            (SELECT COUNT(*) FROM registros_ingreso r WHERE r.shiftId = t.id AND r.salidaAt IS NOT NULL)
              + (SELECT COUNT(*) FROM registros_bicicleta b WHERE b.shiftId = t.id AND b.salidaAt IS NOT NULL) AS totalSalidas
        FROM turnos t
        ORDER BY t.iniciadoAt DESC
        """
    )
    fun observeSummaries(): Flow<List<ShiftSummary>>

    /** Entradas y salidas por punto de control, para ingresos ocurridos dentro de `[from, to]`. */
    @Query(
        """
        SELECT controlPoint, SUM(entradas) AS entradas, SUM(salidas) AS salidas FROM (
            SELECT t.controlPoint AS controlPoint, COUNT(*) AS entradas,
                SUM(CASE WHEN r.salidaAt IS NOT NULL THEN 1 ELSE 0 END) AS salidas
            FROM registros_ingreso r JOIN turnos t ON t.id = r.shiftId
            WHERE r.entradaAt BETWEEN :from AND :to GROUP BY t.controlPoint
            UNION ALL
            SELECT t.controlPoint AS controlPoint, COUNT(*) AS entradas,
                SUM(CASE WHEN b.salidaAt IS NOT NULL THEN 1 ELSE 0 END) AS salidas
            FROM registros_bicicleta b JOIN turnos t ON t.id = b.shiftId
            WHERE b.entradaAt BETWEEN :from AND :to GROUP BY t.controlPoint
        ) GROUP BY controlPoint
        """
    )
    suspend fun countByPoint(from: Long, to: Long): List<PointCount>
}
