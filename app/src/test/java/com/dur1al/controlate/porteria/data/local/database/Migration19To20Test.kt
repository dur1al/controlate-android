package com.dur1al.controlate.porteria.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 19 -> 20: los puntos de control pasan a una tabla con los puntos de fábrica precargados. */
@RunWith(AndroidJUnit4::class)
class Migration19To20Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun creates_control_points_with_factory_defaults_and_keeps_shifts() {
        helper.createDatabase(DB, 19).use { db ->
            db.execSQL(
                "INSERT INTO turnos (guardia, controlPoint, tipo, fecha, iniciadoAt) " +
                    "VALUES ('Guardia Demo', 'porteria_3', 'DAY', '2026-06-14', 1)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB, 20, true, AppDatabase.MIGRATION_19_20)

        db.query("SELECT id, nombre, tipo, orden, activo FROM puntos_control ORDER BY tipo DESC, orden").use { c ->
            val rows = buildList {
                while (c.moveToNext()) add(listOf(c.getString(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4)))
            }
            assertEquals(ControlPoint.DEFAULTS.map { listOf(it.id, it.name, it.kind.name, it.order, 1) }, rows)
        }
        db.query("SELECT controlPoint FROM turnos").use { c ->
            c.moveToFirst()
            assertEquals("porteria_3", c.getString(0))
        }
        db.close()
    }

    @Test
    fun seeding_defaults_twice_does_not_duplicate_or_overwrite() {
        helper.createDatabase(DB, 19).close()
        val db = helper.runMigrationsAndValidate(DB, 20, true, AppDatabase.MIGRATION_19_20)
        db.execSQL("UPDATE puntos_control SET nombre = 'Acceso Norte' WHERE id = 'porteria_2'")

        AppDatabase.insertDefaultControlPoints(db)

        db.query("SELECT COUNT(*) FROM puntos_control").use { c ->
            c.moveToFirst()
            assertEquals(ControlPoint.DEFAULTS.size, c.getInt(0))
        }
        db.query("SELECT nombre FROM puntos_control WHERE id = 'porteria_2'").use { c ->
            c.moveToFirst()
            assertEquals("Acceso Norte", c.getString(0))
        }
        db.close()
    }

    private companion object {
        const val DB = "migration-19-20-test"
    }
}
