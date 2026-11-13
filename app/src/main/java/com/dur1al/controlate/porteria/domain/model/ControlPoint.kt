package com.dur1al.controlate.porteria.domain.model

/**
 * Punto de control físico. [id] es la clave técnica persistida (inmutable);
 * nombre y color visibles se resuelven en la capa de presentación.
 */
enum class ControlPoint(val id: String, val kind: Kind) {
    PORTERIA_2("porteria_2", Kind.PORTERIA),
    PORTERIA_3("porteria_3", Kind.PORTERIA),
    PORTERIA_4("porteria_4", Kind.PORTERIA),
    BICICLETERO_1("bicicletero_1", Kind.BICICLETERO),
    BICICLETERO_2("bicicletero_2", Kind.BICICLETERO);

    enum class Kind { PORTERIA, BICICLETERO }

    companion object {
        /** Resuelve por id técnico (sin distinguir mayúsculas, por datos heredados). */
        fun fromId(id: String?): ControlPoint? =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) }

        fun ofKind(kind: Kind): List<ControlPoint> = entries.filter { it.kind == kind }
    }
}
