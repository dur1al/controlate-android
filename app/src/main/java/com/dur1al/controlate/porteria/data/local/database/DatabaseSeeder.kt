package com.dur1al.controlate.porteria.data.local.database

import android.content.res.Resources
import androidx.annotation.RawRes
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Carga datos iniciales 100 % sintéticos desde `res/raw/`.
 *
 * Directorio demo: solo en una instalación nueva (no se reinyecta si el usuario lo vació).
 */
class DatabaseSeeder(
    private val database: () -> AppDatabase,
    private val resources: Resources,
    private val gson: Gson = Gson()
) {
    private val freshInstall = AtomicBoolean(false)

    /** Llamado por el callback `onCreate` de Room. */
    fun onDatabaseCreated() = freshInstall.set(true)

    suspend fun seedIfNeeded() {
        val db = database()
        // La primera consulta abre la base; si se creó desde cero, `onDatabaseCreated` ya corrió.
        val personas = db.personaDao().count()
        if (freshInstall.getAndSet(false) && personas == 0) {
            db.personaDao().upsertAll(read<List<Persona>>(R.raw.seed_personas))
        }
    }

    private inline fun <reified T> read(@RawRes res: Int): T =
        resources.openRawResource(res).bufferedReader().use { reader ->
            gson.fromJson(reader, object : TypeToken<T>() {}.type)
        }
}
