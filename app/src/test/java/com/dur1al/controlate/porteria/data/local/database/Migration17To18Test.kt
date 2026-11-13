package com.dur1al.controlate.porteria.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class Migration17To18Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private val zone = ZoneId.of("America/Santiago")
    private fun epoch(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    private val nightShift = epoch(2026, 6, 14, 20, 0)
    private val bikeShift = epoch(2026, 6, 14, 8, 0)
    private val activeShift = epoch(2026, 6, 15, 8, 0)

    @Test
    fun migrates_legacy_rows_into_turnos_and_epoch_timestamps() {
        helper.createDatabase(DB, 17).use { db -> seedLegacyRows(db) }

        val legacy = LegacyActiveShift(
            id = activeShift,
            guardia = "Guardia Activo",
            controlPoint = "porteria_3",
            turnoTipo = "DIA (08:00 - 20:00)",
            fecha = "2026-06-15"
        )
        val db = helper.runMigrationsAndValidate(DB, 18, true, Migration17To18(legacy, zone))

        db.query("SELECT id, guardia, controlPoint, tipo, fecha, iniciadoAt, finalizadoAt FROM turnos ORDER BY id").use { c ->
            assertEquals(3, c.count)
            c.moveToNext()
            assertEquals(bikeShift, c.getLong(0))
            assertEquals("bicicletero_1", c.getString(2))
            assertEquals("DAY", c.getString(3))
            assertEquals("2026-06-14", c.getString(4))
            assertNotNull(c.getLong(6))
            c.moveToNext()
            assertEquals(nightShift, c.getLong(0))
            assertEquals("Guardia Noche", c.getString(1))
            assertEquals("porteria_2", c.getString(2))
            assertEquals("NIGHT", c.getString(3))
            assertEquals(nightShift, c.getLong(5))
            c.moveToNext()
            assertEquals(activeShift, c.getLong(0))
            assertEquals("Guardia Activo", c.getString(1))
            assertEquals(true, c.isNull(6))
        }

        db.query("SELECT entradaAt, salidaAt, tipoPersona, rut FROM registros_ingreso").use { c ->
            c.moveToFirst()
            assertEquals(epoch(2026, 6, 14, 21, 0), c.getLong(0))
            // La salida a las 02:30 ocurre al día siguiente.
            assertEquals(epoch(2026, 6, 15, 2, 30), c.getLong(1))
            assertEquals("FUNCIONARIO", c.getString(2))
            assertEquals("111111111", c.getString(3))
        }

        db.query("SELECT entradaAt, salidaAt, tipoPersona, observacion FROM registros_bicicleta").use { c ->
            c.moveToFirst()
            assertEquals(epoch(2026, 6, 14, 8, 15), c.getLong(0))
            assertEquals(true, c.isNull(1))
            assertEquals("ESTUDIANTE", c.getString(2))
            assertEquals("Roja", c.getString(3))
        }

        db.query("SELECT COUNT(*) FROM examen_preguntas").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
        db.query("SELECT realizadoAt FROM examen_resultados").use { c ->
            c.moveToFirst()
            assertEquals(epoch(2026, 6, 14, 10, 30), c.getLong(0))
        }
        db.query("SELECT tipoPersona FROM personas").use { c ->
            c.moveToFirst()
            assertEquals("FUNCIONARIO", c.getString(0))
        }
        db.close()
    }

    @Test
    fun migration_without_active_shift_closes_every_legacy_turno() {
        helper.createDatabase(DB, 17).use { db -> seedLegacyRows(db) }
        val db = helper.runMigrationsAndValidate(DB, 18, true, Migration17To18(null, zone))
        db.query("SELECT COUNT(*) FROM turnos WHERE finalizadoAt IS NULL").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.query("SELECT guardia FROM turnos WHERE id = ?", arrayOf<Any>(activeShift)).use { c ->
            assertEquals(0, c.count)
        }
        db.close()
    }

    @Test
    fun unparseable_legacy_time_falls_back_to_shift_start() {
        helper.createDatabase(DB, 17).use { db ->
            db.execSQL(
                "INSERT INTO registros_ingreso (porteria, guardiaTurno, fechaTurno, turnoTipo, shiftId, patente, " +
                    "nombreApellido, departamento, descripcionAuto, horaEntrada, fechaEntrada, esPadron) " +
                    "VALUES ('porteria_2','G','2026-06-14','DIA (08:00 - 20:00)',$nightShift,'DEMO01','Ana','A','B','??','',0)"
            )
        }
        val db = helper.runMigrationsAndValidate(DB, 18, true, Migration17To18(null, zone))
        db.query("SELECT entradaAt, salidaAt FROM registros_ingreso").use { c ->
            c.moveToFirst()
            assertEquals(nightShift, c.getLong(0))
            assertNull(if (c.isNull(1)) null else c.getLong(1))
        }
        db.close()
    }

    private fun seedLegacyRows(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO registros_ingreso (porteria, guardiaTurno, fechaTurno, turnoTipo, shiftId, patente, " +
                "nombreApellido, rut, departamento, descripcionAuto, tipoPersona, horaEntrada, fechaEntrada, horaSalida, esPadron) " +
                "VALUES ('porteria_2','Guardia Noche','2026-06-14','NOCHE (20:00 - 08:00)',$nightShift,'DEMO01'," +
                "'Ana Torres','11.111.111-1','Adm','Sedan','Funcionario','21:00','2026-06-14','02:30',0)"
        )
        db.execSQL(
            "INSERT INTO registros_bicicleta (shiftId, rut, nombre, carrera, tipoPersona, controlPoint, " +
                "observacionBicicleta, fecha, horaEntrada, horaSalida, estado, guardiaTurno, turnoTipo) " +
                "VALUES ($bikeShift,'111111111','Ana','Ing','ESTUDIANTE','BICICLETERO_1','Roja','14/06/2026','08:15',NULL," +
                "'ENTRADA','Guardia Bici','DIA (08:00 - 20:00)')"
        )
        repeat(2) {
            db.execSQL(
                "INSERT INTO examen_preguntas (pregunta, opcionA, opcionB, opcionC, opcionD, respuestaCorrecta, categoria, explicacion) " +
                    "VALUES ('P1','a','b','c','d',1,'General','')"
            )
        }
        db.execSQL(
            "INSERT INTO examen_resultados (guardiaNombre, fecha, puntaje, totalPreguntas, porcentaje, aprobado, tiempoEmpleadoSegundos) " +
                "VALUES ('G','14/06/2026 10:30',42,60,70.0,1,100)"
        )
        db.execSQL(
            "INSERT INTO personas (patente, nombreApellido, departamento, descripcionAuto, tipoPersona) " +
                "VALUES ('DEMO01','Ana Torres','Adm','Sedan','Funcionario')"
        )
    }

    private companion object {
        const val DB = "migration-test"
    }
}
