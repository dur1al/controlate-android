package com.dur1al.controlate.porteria.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Ingreso al bicicletero. Mismo modelo temporal que [RegistroIngreso]. */
@Entity(
    tableName = "registros_bicicleta",
    foreignKeys = [
        ForeignKey(
            entity = Turno::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("shiftId"), Index("rut"), Index("entradaAt")]
)
data class RegistroBicicleta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val rut: String,
    val nombre: String,
    val carrera: String,
    val tipoPersona: String,
    val observacion: String? = null,
    val entradaAt: Long,
    val salidaAt: Long? = null
)
