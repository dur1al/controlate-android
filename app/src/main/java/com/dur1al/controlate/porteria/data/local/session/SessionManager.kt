package com.dur1al.controlate.porteria.data.local.session

import android.content.Context
import android.content.SharedPreferences
import com.dur1al.controlate.porteria.data.local.database.LegacyActiveShift
import com.dur1al.controlate.porteria.domain.repository.SessionStore

/**
 * Guarda solo el puntero al turno abierto. Los datos del turno (guardia, punto, tipo)
 * viven en la tabla `turnos`; así no hay dos fuentes de verdad.
 */
class SessionManager(context: Context) : SessionStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun activeShiftId(): Long? =
        prefs.getLong(KEY_ACTIVE_SHIFT_ID, NO_SHIFT).takeIf { it != NO_SHIFT }

    override fun setActiveShift(id: Long) {
        prefs.edit().putLong(KEY_ACTIVE_SHIFT_ID, id).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_ACTIVE_SHIFT_ID).apply()
    }

    /**
     * Lee (una vez) el turno abierto que guardaban versiones anteriores en estas mismas
     * preferencias, lo apunta como turno activo y borra las claves heredadas.
     * Debe llamarse antes de abrir la base para que [com.dur1al.controlate.porteria.data.local.database.Migration17To18]
     * pueda materializarlo en la tabla `turnos`.
     */
    fun consumeLegacyActiveShift(): LegacyActiveShift? {
        if (!prefs.contains(LEGACY_IS_ACTIVE)) return null
        val active = prefs.getBoolean(LEGACY_IS_ACTIVE, false)
        val id = prefs.getLong(LEGACY_SHIFT_ID, 0L)
        val legacy = if (active && id > 0) {
            LegacyActiveShift(
                id = id,
                guardia = prefs.getString(LEGACY_GUARDIA, "").orEmpty(),
                controlPoint = prefs.getString(LEGACY_POINT_ID, "").orEmpty(),
                turnoTipo = prefs.getString(LEGACY_TURNO_TIPO, "").orEmpty(),
                fecha = prefs.getString(LEGACY_FECHA, "").orEmpty()
            )
        } else {
            null
        }
        prefs.edit().apply {
            LEGACY_KEYS.forEach(::remove)
            if (legacy != null) putLong(KEY_ACTIVE_SHIFT_ID, legacy.id)
        }.apply()
        return legacy
    }

    private companion object {
        const val PREFS_NAME = "porteria_prefs"
        const val KEY_ACTIVE_SHIFT_ID = "active_shift_id"
        const val NO_SHIFT = -1L

        const val LEGACY_GUARDIA = "guardia_name"
        const val LEGACY_POINT_ID = "porteria_id"
        const val LEGACY_FECHA = "fecha_turno"
        const val LEGACY_TURNO_TIPO = "turno_tipo"
        const val LEGACY_SHIFT_ID = "shift_id"
        const val LEGACY_IS_ACTIVE = "is_shift_active"
        val LEGACY_KEYS = listOf(
            LEGACY_GUARDIA, LEGACY_POINT_ID, "porteria_name", LEGACY_FECHA,
            LEGACY_TURNO_TIPO, LEGACY_SHIFT_ID, LEGACY_IS_ACTIVE
        )
    }
}
