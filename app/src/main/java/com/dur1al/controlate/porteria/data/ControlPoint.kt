package com.dur1al.controlate.porteria.data

import com.dur1al.controlate.R

/**
 * Representa un punto de control en la infraestructura.
 * El ID es inmutable para la base de datos (clave técnica),
 * mientras que el labelRes permite cambiar el nombre visible sin romper la lógica.
 *
 * Cada punto tiene un color primario único para identificación visual rápida.
 */
enum class ControlPoint(
    val id: String,
    val labelRes: Int,
    val primaryColorRes: Int
) {
    ACCESO_1("acceso_1", R.string.acceso_1, R.color.point_acceso_1),
    PORTERIA_2("porteria_2", R.string.porteria_2, R.color.point_porteria_2),
    PORTERIA_3("porteria_3", R.string.porteria_3, R.color.point_porteria_3),
    PORTERIA_4("porteria_4", R.string.porteria_4, R.color.point_porteria_4),
    ESTACIONAMIENTO("estacionamiento", R.string.estacionamiento, R.color.point_estacionamiento),
    BICICLETERO_1("bicicletero_1", R.string.bicicletero_1, R.color.point_bicicletero_1),
    BICICLETERO_2("bicicletero_2", R.string.bicicletero_2, R.color.point_bicicletero_2);

    companion object {
        fun fromId(id: String?): ControlPoint {
            return entries.find { it.id == id } ?: PORTERIA_2
        }

        fun getAllIds(): List<String> = entries.map { it.id }
    }
}

