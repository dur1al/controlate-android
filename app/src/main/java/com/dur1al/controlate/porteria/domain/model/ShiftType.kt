package com.dur1al.controlate.porteria.domain.model

/**
 * Tipo de turno operativo. Se persiste el [name]; la etiqueta visible vive en `strings.xml`.
 */
enum class ShiftType {
    DAY,
    NIGHT;

    companion object {
        /** Acepta el código o las etiquetas heredadas (`"NOCHE (20:00 - 08:00)"`). Desconocido -> [DAY]. */
        fun fromCode(code: String?): ShiftType {
            val value = code?.trim()?.uppercase().orEmpty()
            return when {
                value == NIGHT.name || value.startsWith("NOCHE") -> NIGHT
                else -> DAY
            }
        }
    }
}
