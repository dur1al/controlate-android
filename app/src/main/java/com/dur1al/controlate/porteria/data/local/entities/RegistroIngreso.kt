package com.dur1al.controlate.porteria.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ingreso vehicular. Los instantes se guardan en epoch millis ([entradaAt], [salidaAt]);
 * fecha y hora visibles se derivan al mostrar, lo que evita errores en turnos que cruzan medianoche.
 *
 * @property rut forma canónica de [com.dur1al.controlate.porteria.domain.validation.Rut.normalize].
 * @property tipoPersona código de [com.dur1al.controlate.porteria.domain.model.PersonaType].
 */
@Entity(
    tableName = "registros_ingreso",
    foreignKeys = [
        ForeignKey(
            entity = Turno::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("shiftId"), Index("patente"), Index("entradaAt")]
)
data class RegistroIngreso(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val patente: String,
    val nombreApellido: String,
    val rut: String? = null,
    val departamento: String,
    val descripcionAuto: String,
    val empresa: String? = null,
    val tipoPersona: String,
    val observaciones: String? = null,
    val padron: String? = null,
    val esPadron: Boolean = false,
    val entradaAt: Long,
    val salidaAt: Long? = null
)
