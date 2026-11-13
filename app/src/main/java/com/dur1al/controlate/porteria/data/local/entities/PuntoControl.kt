package com.dur1al.controlate.porteria.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila de `puntos_control`. Se mapea a [com.dur1al.controlate.porteria.domain.model.ControlPoint]
 * en el repositorio porque el tipo se guarda como código de texto estable.
 *
 * @property id clave técnica referenciada por `turnos.controlPoint`.
 * @property tipo `ControlPoint.Kind.name`.
 */
@Entity(tableName = "puntos_control")
data class PuntoControl(
    @PrimaryKey val id: String,
    val nombre: String,
    val tipo: String,
    val orden: Int,
    val activo: Boolean
)
