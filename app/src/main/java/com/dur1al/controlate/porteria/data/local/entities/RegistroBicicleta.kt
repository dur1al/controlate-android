package com.dur1al.controlate.porteria.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registros_bicicleta")
data class RegistroBicicleta(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val shiftId: Long,
    val rut: String,
    val nombre: String,
    val carrera: String,
    val tipoPersona: String,
    val controlPoint: String,
    val observacionBicicleta: String?,
    val fecha: String,
    val horaEntrada: String,
    val horaSalida: String? = null,
    val estado: String = "ENTRADA", // "ENTRADA" o "SALIDA"
    val guardiaTurno: String = "No registrado",
    val turnoTipo: String = "No definido"
)
