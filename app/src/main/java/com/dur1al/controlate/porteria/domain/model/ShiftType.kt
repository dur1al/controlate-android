package com.dur1al.controlate.porteria.domain.model

import com.dur1al.controlate.R

/**
 * Tipos de turno operativo.
 * Los valores de [displayValue] se usan para persistencia y reportes.
 */
enum class ShiftType(val displayValue: String, val labelRes: Int) {
    DAY("DIA (08:00 - 20:00)", R.string.shift_type_day),
    NIGHT("NOCHE (20:00 - 08:00)", R.string.shift_type_night);

    companion object {
        fun fromValue(value: String?): ShiftType {
            return entries.find { it.displayValue == value } ?: DAY
        }
    }
}
