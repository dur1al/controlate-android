package com.dur1al.controlate.porteria.domain.model

/**
 * Punto de control físico (portería o bicicletero), configurable por el administrador.
 *
 * - [id] es la clave técnica persistida en cada turno: inmutable y nunca se reutiliza.
 * - [name] es lo que ven guardia y administrador; puede renombrarse sin afectar el historial.
 * - [order] fija el orden en listas y el color de identidad del punto.
 * - Un punto inactivo no se ofrece para abrir turnos, pero sus turnos y reportes se conservan.
 */
data class ControlPoint(
    val id: String,
    val name: String,
    val kind: Kind,
    val order: Int,
    val active: Boolean = true
) {
    enum class Kind(val idPrefix: String) {
        PORTERIA("porteria"),
        BICICLETERO("bicicletero");

        companion object {
            fun fromCode(code: String?): Kind? = entries.firstOrNull { it.name.equals(code?.trim(), ignoreCase = true) }

            /** Deduce el tipo de un id heredado cuando el punto ya no existe en la tabla. */
            fun fromId(id: String?): Kind? = entries.firstOrNull { id?.trim()?.startsWith(it.idPrefix, ignoreCase = true) == true }
        }
    }

    companion object {
        /**
         * Puntos de fábrica. Las porterías se muestran como "Portería 1..3" pero conservan los ids
         * históricos `porteria_2..4`: cambiarlos dejaría huérfanos los turnos ya guardados.
         */
        val DEFAULTS = listOf(
            ControlPoint("porteria_2", "Portería 1", Kind.PORTERIA, 0),
            ControlPoint("porteria_3", "Portería 2", Kind.PORTERIA, 1),
            ControlPoint("porteria_4", "Portería 3", Kind.PORTERIA, 2),
            ControlPoint("bicicletero_1", "Punto Bicicleta 1", Kind.BICICLETERO, 0),
            ControlPoint("bicicletero_2", "Punto Bicicleta 2", Kind.BICICLETERO, 1)
        )
    }
}
