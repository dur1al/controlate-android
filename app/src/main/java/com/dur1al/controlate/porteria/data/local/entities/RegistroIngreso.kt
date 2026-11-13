package com.dur1al.controlate.porteria.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registros_ingreso",
    indices = [Index(value = ["patente"]), Index(value = ["shiftId"])]
)
data class RegistroIngreso(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val porteria: String,
    val guardiaTurno: String,
    val fechaTurno: String,
    val turnoTipo: String = "No definido",
    val shiftId: Long = 0L,
    val patente: String,
    val nombreApellido: String,
    val rut: String? = null,
    val departamento: String,
    val descripcionAuto: String,
    val empresa: String? = null,
    val tipoPersona: String? = null,
    val observaciones: String? = null,
    val horaEntrada: String,
    val fechaEntrada: String,
    val horaSalida: String? = null,
    val padron: String? = null,
    val esPadron: Boolean = false
)
