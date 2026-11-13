package com.dur1al.controlate.porteria.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.local.dao.BicicletaDao
import com.dur1al.controlate.porteria.data.local.dao.ExamenDao
import com.dur1al.controlate.porteria.data.local.dao.PersonaDao
import com.dur1al.controlate.porteria.data.local.dao.RegistroDao
import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.data.local.entities.ExamenResultado
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Base de datos principal de la aplicación Controlate.
 *
 * Usa Room con migraciones explícitas. El seed inicial de personas
 * se carga desde [R.raw.seed_personas] en la primera creación.
 */
@Database(
    entities = [
        RegistroIngreso::class,
        Persona::class,
        RegistroBicicleta::class,
        ExamenPregunta::class,
        ExamenResultado::class
    ],
    version = 17,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun registroDao(): RegistroDao
    abstract fun personaDao(): PersonaDao
    abstract fun bicicletaDao(): BicicletaDao
    abstract fun examenDao(): ExamenDao

    companion object {
        private const val DATABASE_NAME = "porteria_db"
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(
                        createMigration(12, 13),
                        createMigration(13, 14),
                        createMigration(14, 15),
                        createMigration(15, 16),
                        MIGRATION_16_17
                    )
                    .addCallback(SeedCallback(context.applicationContext))
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // region Migrations

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `examen_preguntas` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `pregunta` TEXT NOT NULL,
                        `opcionA` TEXT NOT NULL,
                        `opcionB` TEXT NOT NULL,
                        `opcionC` TEXT NOT NULL,
                        `opcionD` TEXT NOT NULL,
                        `respuestaCorrecta` INTEGER NOT NULL,
                        `categoria` TEXT NOT NULL,
                        `explicacion` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `examen_resultados` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `guardiaNombre` TEXT NOT NULL,
                        `fecha` TEXT NOT NULL,
                        `puntaje` INTEGER NOT NULL,
                        `totalPreguntas` INTEGER NOT NULL,
                        `porcentaje` REAL NOT NULL,
                        `aprobado` INTEGER NOT NULL,
                        `tiempoEmpleadoSegundos` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private fun createMigration(startVersion: Int, endVersion: Int): Migration {
            return object : Migration(startVersion, endVersion) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    ensureBicicletaColumns(db)
                }
            }
        }

        private fun ensureBicicletaColumns(db: SupportSQLiteDatabase) {
            if (!columnExists(db, "registros_bicicleta", "shiftId")) {
                db.execSQL("ALTER TABLE registros_bicicleta ADD COLUMN shiftId INTEGER NOT NULL DEFAULT 0")
            }
            if (!columnExists(db, "registros_bicicleta", "tipoPersona")) {
                db.execSQL("ALTER TABLE registros_bicicleta ADD COLUMN tipoPersona TEXT NOT NULL DEFAULT ''")
            }
            if (!columnExists(db, "registros_bicicleta", "controlPoint")) {
                db.execSQL("ALTER TABLE registros_bicicleta ADD COLUMN controlPoint TEXT NOT NULL DEFAULT 'BICICLETERO_1'")
            }
            if (!columnExists(db, "registros_bicicleta", "guardiaTurno")) {
                db.execSQL("ALTER TABLE registros_bicicleta ADD COLUMN guardiaTurno TEXT NOT NULL DEFAULT 'No registrado'")
            }
            if (!columnExists(db, "registros_bicicleta", "turnoTipo")) {
                db.execSQL("ALTER TABLE registros_bicicleta ADD COLUMN turnoTipo TEXT NOT NULL DEFAULT 'No definido'")
            }
        }

        private fun columnExists(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String
        ): Boolean {
            db.query("PRAGMA table_info($tableName)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex == -1) return false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                        return true
                    }
                }
            }
            return false
        }

        // endregion
    }

    /**
     * Callback que carga datos iniciales desde [R.raw.seed_personas]
     * la primera vez que se crea la base de datos.
     */
    private class SeedCallback(private val context: Context) : Callback() {
        private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            dbScope.launch {
                loadSeedPersonas()
                loadSeedPreguntas()
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            dbScope.launch {
                loadSeedPreguntasIfEmpty()
            }
        }

        private suspend fun loadSeedPersonas() {
            runCatching {
                val json = context.resources
                    .openRawResource(R.raw.seed_personas)
                    .bufferedReader()
                    .use { it.readText() }
                val type = object : TypeToken<List<Persona>>() {}.type
                val personas: List<Persona> = Gson().fromJson(json, type)
                getDatabase(context).personaDao().insertAll(personas)
                Log.d(TAG, "Seed completado: ${personas.size} personas cargadas")
            }.onFailure { e ->
                Log.e(TAG, "Error al cargar seed de personas", e)
            }
        }

        private suspend fun loadSeedPreguntas() {
            runCatching {
                val json = context.resources
                    .openRawResource(R.raw.seed_preguntas)
                    .bufferedReader()
                    .use { it.readText() }
                val type = object : TypeToken<List<ExamenPregunta>>() {}.type
                val preguntas: List<ExamenPregunta> = Gson().fromJson(json, type)
                getDatabase(context).examenDao().insertPreguntas(preguntas)
                Log.d(TAG, "Seed completado: ${preguntas.size} preguntas OS-10 cargadas")
            }.onFailure { e ->
                Log.e(TAG, "Error al cargar seed de preguntas", e)
            }
        }

        private suspend fun loadSeedPreguntasIfEmpty() {
            runCatching {
                val dao = getDatabase(context).examenDao()
                if (dao.getPreguntasCount() == 0) {
                    loadSeedPreguntas()
                }
            }
        }
    }
}
