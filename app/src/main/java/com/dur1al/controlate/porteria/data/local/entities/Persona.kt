package com.dur1al.controlate.porteria.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Directorio de personas conocidas, usado para autocompletar formularios.
 * La patente es única; el RUT se indexa para búsquedas rápidas.
 */
@Entity(
    tableName = "personas",
    indices = [Index(value = ["patente"], unique = true), Index("rut")]
)
data class Persona(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patente: String,
    val nombreApellido: String,
    val departamento: String,
    val descripcionAuto: String,
    val empresa: String? = null,
    val rut: String? = null,
    val tipoPersona: String? = null,
    val padron: String? = null
)
