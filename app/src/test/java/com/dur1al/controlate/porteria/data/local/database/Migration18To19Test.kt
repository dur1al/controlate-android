package com.dur1al.controlate.porteria.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 18 -> 19: retiro del simulador OS-10 sin tocar los datos operativos. */
@RunWith(AndroidJUnit4::class)
class Migration18To19Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun drops_exam_tables_and_keeps_operational_data() {
        helper.createDatabase(DB, 18).use { db ->
            db.execSQL(
                "INSERT INTO personas (patente, nombreApellido, departamento, descripcionAuto) " +
                    "VALUES ('DEMO01','Persona Demo','Adm','Sedan')"
            )
            db.execSQL(
                "INSERT INTO examen_preguntas (pregunta, opcionA, opcionB, opcionC, opcionD, respuestaCorrecta, categoria, explicacion) " +
                    "VALUES ('P1','a','b','c','d',1,'General','')"
            )
        }

        val db = helper.runMigrationsAndValidate(DB, 19, true, AppDatabase.MIGRATION_18_19)

        db.query("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name LIKE 'examen_%'").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM personas").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
        db.close()
    }

    private companion object {
        const val DB = "migration-18-19-test"
    }
}
