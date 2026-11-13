package com.dur1al.controlate.porteria.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Resultado de un intento del simulador OS-10. */
@Entity(tableName = "examen_resultados")
data class ExamenResultado(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val guardiaNombre: String,
    val realizadoAt: Long,
    val puntaje: Int,
    val totalPreguntas: Int,
    val porcentaje: Float,
    val aprobado: Boolean,
    val tiempoEmpleadoSegundos: Long = 0
)
