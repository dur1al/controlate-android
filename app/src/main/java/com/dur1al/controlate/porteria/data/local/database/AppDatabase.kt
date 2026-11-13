package com.dur1al.controlate.porteria.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dur1al.controlate.porteria.data.local.dao.BicicletaDao
import com.dur1al.controlate.porteria.data.local.dao.PersonaDao
import com.dur1al.controlate.porteria.data.local.dao.PuntoControlDao
import com.dur1al.controlate.porteria.data.local.dao.RegistroDao
import com.dur1al.controlate.porteria.data.local.dao.TurnoDao
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.PuntoControl
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.model.ControlPoint

/**
 * Base de datos local (fuente única de verdad, offline-first).
 *
 * El esquema se exporta a `app/schemas/` y cada migración se cubre con un test
 * (`Migration17To18Test`, `Migration18To19Test`, `Migration19To20Test`). Versiones anteriores a la 12 no tienen ruta de migración:
 * la app nunca se distribuyó públicamente con ellas.
 */
@Database(
    entities = [
        Turno::class,
        RegistroIngreso::class,
        Persona::class,
        RegistroBicicleta::class,
        PuntoControl::class
    ],
    version = 20,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun turnoDao(): TurnoDao
    abstract fun registroDao(): RegistroDao
    abstract fun personaDao(): PersonaDao
    abstract fun bicicletaDao(): BicicletaDao
    abstract fun puntoControlDao(): PuntoControlDao

    companion object {
        const val DATABASE_NAME = "porteria_db"

        /**
         * Construye la base. [legacyActiveShift] es el turno abierto que versiones previas guardaban
         * solo en preferencias; [onCreated] se invoca una única vez tras crear la base desde cero.
         */
        fun build(
            context: Context,
            legacyActiveShift: LegacyActiveShift?,
            onCreated: () -> Unit
        ): AppDatabase = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(*legacyMigrations(), Migration17To18(legacyActiveShift), MIGRATION_18_19, MIGRATION_19_20)
            .addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Los puntos de fábrica se insertan en la misma creación: nunca hay una base sin puntos.
                    insertDefaultControlPoints(db)
                    onCreated()
                }
            })
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

        /** 18 -> 19: se retira el simulador OS-10; sus tablas quedan huérfanas y se eliminan. */
        internal val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `examen_preguntas`")
                db.execSQL("DROP TABLE IF EXISTS `examen_resultados`")
            }
        }

        /** 19 -> 20: los puntos de control pasan de un enum fijo a una tabla administrable. */
        internal val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `puntos_control` (`id` TEXT NOT NULL, `nombre` TEXT NOT NULL, " +
                        "`tipo` TEXT NOT NULL, `orden` INTEGER NOT NULL, `activo` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                insertDefaultControlPoints(db)
            }
        }

        /** Inserta los puntos de fábrica sin pisar los existentes (consultas parametrizadas). */
        internal fun insertDefaultControlPoints(db: SupportSQLiteDatabase) {
            ControlPoint.DEFAULTS.forEach { p ->
                db.execSQL(
                    "INSERT OR IGNORE INTO puntos_control (id, nombre, tipo, orden, activo) VALUES (?, ?, ?, ?, 1)",
                    arrayOf<Any>(p.id, p.name, p.kind.name, p.order)
                )
            }
        }

        /** Migraciones 12 -> 17: aseguran columnas agregadas en distintas versiones del bicicletero. */
        internal fun legacyMigrations(): Array<Migration> = arrayOf(
            ensureBicicletaColumns(12, 13),
            ensureBicicletaColumns(13, 14),
            ensureBicicletaColumns(14, 15),
            ensureBicicletaColumns(15, 16),
            MIGRATION_16_17
        )

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `examen_preguntas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`pregunta` TEXT NOT NULL, `opcionA` TEXT NOT NULL, `opcionB` TEXT NOT NULL, `opcionC` TEXT NOT NULL, " +
                        "`opcionD` TEXT NOT NULL, `respuestaCorrecta` INTEGER NOT NULL, `categoria` TEXT NOT NULL, " +
                        "`explicacion` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `examen_resultados` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`guardiaNombre` TEXT NOT NULL, `fecha` TEXT NOT NULL, `puntaje` INTEGER NOT NULL, " +
                        "`totalPreguntas` INTEGER NOT NULL, `porcentaje` REAL NOT NULL, `aprobado` INTEGER NOT NULL, " +
                        "`tiempoEmpleadoSegundos` INTEGER NOT NULL)"
                )
            }
        }

        private fun ensureBicicletaColumns(from: Int, to: Int) = object : Migration(from, to) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = mapOf(
                    "shiftId" to "INTEGER NOT NULL DEFAULT 0",
                    "tipoPersona" to "TEXT NOT NULL DEFAULT ''",
                    "controlPoint" to "TEXT NOT NULL DEFAULT 'bicicletero_1'",
                    "guardiaTurno" to "TEXT NOT NULL DEFAULT 'No registrado'",
                    "turnoTipo" to "TEXT NOT NULL DEFAULT 'DAY'"
                )
                val existing = db.query("PRAGMA table_info(registros_bicicleta)").use { c ->
                    val nameIndex = c.getColumnIndexOrThrow("name")
                    buildSet { while (c.moveToNext()) add(c.getString(nameIndex).lowercase()) }
                }
                columns.filterKeys { it.lowercase() !in existing }.forEach { (name, definition) ->
                    db.execSQL("ALTER TABLE registros_bicicleta ADD COLUMN $name $definition")
                }
            }
        }
    }
}
