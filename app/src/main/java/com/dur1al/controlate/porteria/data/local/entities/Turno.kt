package com.dur1al.controlate.porteria.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Turno de un guardia en un punto de control. Es la raíz de agregación de los registros:
 * cada [RegistroIngreso] y [RegistroBicicleta] pertenece a exactamente un turno.
 *
 * @property controlPoint id técnico de [com.dur1al.controlate.porteria.domain.model.ControlPoint].
 * @property tipo código de [com.dur1al.controlate.porteria.domain.model.ShiftType].
 * @property fecha día local de inicio en formato ISO (`yyyy-MM-dd`), para agrupar y filtrar.
 * @property finalizadoAt `null` mientras el turno está abierto.
 */
@Entity(tableName = "turnos", indices = [Index("iniciadoAt")])
data class Turno(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val guardia: String,
    val controlPoint: String,
    val tipo: String,
    val fecha: String,
    val iniciadoAt: Long,
    val finalizadoAt: Long? = null
)
