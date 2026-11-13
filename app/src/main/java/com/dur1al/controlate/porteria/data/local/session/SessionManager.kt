package com.dur1al.controlate.porteria.data.local.session

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("porteria_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GUARDIA = "guardia_name"
        private const val KEY_PORTERIA_ID = "porteria_id"
        private const val KEY_PORTERIA_NAME = "porteria_name"
        private const val KEY_FECHA_TURNO = "fecha_turno"
        private const val KEY_TURNO_TIPO = "turno_tipo"
        private const val KEY_SHIFT_ID = "shift_id"
        private const val KEY_IS_SHIFT_ACTIVE = "is_shift_active"
    }

    /**
     * Inicia un turno guardando el nombre del guardia, el ID técnico del punto de control,
     * el nombre visible del punto, la fecha y el tipo de turno.
     */
    fun startShift(
        guardia: String,
        porteriaName: String,
        porteriaId: String,
        fecha: String,
        turnoTipo: String
    ) {
        val shiftId = System.currentTimeMillis()
        prefs.edit().apply {
            putString(KEY_GUARDIA, guardia)
            putString(KEY_PORTERIA_ID, porteriaId)
            putString(KEY_PORTERIA_NAME, porteriaName)
            putString(KEY_FECHA_TURNO, fecha)
            putString(KEY_TURNO_TIPO, turnoTipo)
            putLong(KEY_SHIFT_ID, shiftId)
            putBoolean(KEY_IS_SHIFT_ACTIVE, true)
            apply()
        }
    }

    fun endShift() {
        prefs.edit().putBoolean(KEY_IS_SHIFT_ACTIVE, false).apply()
    }

    fun isShiftActive(): Boolean = prefs.getBoolean(KEY_IS_SHIFT_ACTIVE, false)
    fun getGuardiaName(): String = prefs.getString(KEY_GUARDIA, "") ?: ""

    /** Devuelve el ID técnico del punto de control (ej: "porteria_2", "bicicletero_1"). */
    fun getPorteriaId(): String = prefs.getString(KEY_PORTERIA_ID, "porteria_2") ?: "porteria_2"

    /** Devuelve el nombre visible del punto de control (ej: "Portería 2", "Bicicletero 1"). */
    fun getPorteriaName(): String {
        // Fallback a KEY_PORTERIA_ID para compatibilidad con datos existentes
        return prefs.getString(KEY_PORTERIA_NAME, null)
            ?: prefs.getString(KEY_PORTERIA_ID, "") ?: ""
    }

    fun getFechaTurno(): String = prefs.getString(KEY_FECHA_TURNO, "") ?: ""
    fun getTurnoTipo(): String = prefs.getString(KEY_TURNO_TIPO, "No definido") ?: "No definido"
    fun getShiftId(): Long = prefs.getLong(KEY_SHIFT_ID, 0L)
}
