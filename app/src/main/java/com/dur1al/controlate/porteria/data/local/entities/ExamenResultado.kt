package com.dur1al.controlate.porteria.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "examen_resultados")
data class ExamenResultado(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val guardiaNombre: String,
    val fecha: String,
    val puntaje: Int,
    val totalPreguntas: Int = 60,
    val porcentaje: Float,
    val aprobado: Boolean,
    val tiempoEmpleadoSegundos: Long = 0
)
