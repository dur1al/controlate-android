package com.dur1al.controlate.porteria.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dur1al.controlate.porteria.data.local.database.AppDatabase
import com.dur1al.controlate.porteria.data.local.database.DatabaseSeeder
import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.model.PointCount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Consultas reales de Room sobre una base en memoria (Robolectric, SQLite nativo). */
@RunWith(AndroidJUnit4::class)
class DaoIntegrationTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun turno(point: String, iniciadoAt: Long, finalizadoAt: Long? = null) =
        db.turnoDao().insert(Turno(guardia = "G", controlPoint = point, tipo = "DAY", fecha = "2026-06-14", iniciadoAt = iniciadoAt, finalizadoAt = finalizadoAt))

    private fun ingreso(shiftId: Long, patente: String, entradaAt: Long, salidaAt: Long? = null, nombre: String = "Ana") =
        RegistroIngreso(shiftId = shiftId, patente = patente, nombreApellido = nombre, departamento = "-", descripcionAuto = "-",
            tipoPersona = "VISITA", entradaAt = entradaAt, salidaAt = salidaAt)

    private fun bici(shiftId: Long, rut: String, entradaAt: Long, salidaAt: Long? = null) =
        RegistroBicicleta(shiftId = shiftId, rut = rut, nombre = "Luis", carrera = "Ing", tipoPersona = "ESTUDIANTE",
            entradaAt = entradaAt, salidaAt = salidaAt)

    @Test
    fun countByPoint_groups_both_tables_inside_the_range_only() = runTest {
        val p2 = turno("porteria_2", 0)
        val b1 = turno("bicicletero_1", 0)
        db.registroDao().insert(ingreso(p2, "AAAA11", entradaAt = 100, salidaAt = 150))
        db.registroDao().insert(ingreso(p2, "BBBB22", entradaAt = 200))
        db.registroDao().insert(ingreso(p2, "CCCC33", entradaAt = 5_000))
        db.bicicletaDao().insert(bici(b1, "111111111", entradaAt = 120))

        val counts = db.turnoDao().countByPoint(0, 1_000).sortedBy { it.controlPoint }

        assertEquals(listOf(PointCount("bicicletero_1", 1, 0), PointCount("porteria_2", 2, 1)), counts)
    }

    @Test
    fun deleting_a_shift_cascades_to_its_records() = runTest {
        val keep = turno("porteria_2", 1)
        val drop = turno("porteria_3", 2, finalizadoAt = 3)
        db.registroDao().insert(ingreso(keep, "AAAA11", 10))
        db.registroDao().insert(ingreso(drop, "BBBB22", 10))

        db.turnoDao().deleteFinished()

        assertEquals(listOf("AAAA11"), db.registroDao().getByShift(keep).map { it.patente })
        assertTrue(db.registroDao().getByShift(drop).isEmpty())
        assertNull(db.turnoDao().getById(drop))
    }

    @Test
    fun summaries_count_records_and_exits_per_shift() = runTest {
        val id = turno("porteria_2", 1)
        db.registroDao().insert(ingreso(id, "AAAA11", 10, salidaAt = 20))
        db.registroDao().insert(ingreso(id, "BBBB22", 11))

        val summary = db.turnoDao().observeSummaries().first().single()

        assertEquals(2, summary.totalRegistros)
        assertEquals(1, summary.totalSalidas)
    }

    @Test
    fun search_treats_percent_and_underscore_literally() = runTest {
        val id = turno("porteria_2", 1)
        db.registroDao().insert(ingreso(id, "AAAA11", 10, nombre = "Ana"))
        db.registroDao().insert(ingreso(id, "BBBB22", 11, nombre = "Luis_100%"))

        assertEquals(listOf("BBBB22"), db.registroDao().observeByShift(id, "%").first().map { it.patente })
        assertEquals(listOf("BBBB22"), db.registroDao().observeByShift(id, "_1").first().map { it.patente })
        assertEquals(2, db.registroDao().observeByShift(id, "").first().size)
        assertEquals(listOf("AAAA11"), db.registroDao().observeByShift(id, "aaa").first().map { it.patente })
    }

    @Test
    fun exit_is_idempotent_and_undo_only_reverts_the_same_instant() = runTest {
        val id = turno("porteria_2", 1)
        val registro = db.registroDao().insert(ingreso(id, "AAAA11", 10))

        assertEquals(1, db.registroDao().markExit(registro, 50))
        assertEquals(0, db.registroDao().markExit(registro, 60))
        assertEquals(0, db.registroDao().undoExit(registro, 60))
        assertEquals(1, db.registroDao().undoExit(registro, 50))
        assertNull(db.registroDao().getById(registro)?.salidaAt)
    }

    @Test
    fun record_with_unknown_shift_is_rejected_by_the_foreign_key() = runTest {
        val failure = runCatching { db.registroDao().insert(ingreso(999, "AAAA11", 10)) }.exceptionOrNull()
        assertTrue("expected FK violation, got $failure", failure is android.database.sqlite.SQLiteConstraintException)
    }

    @Test
    fun insertIfAbsent_never_overwrites_an_existing_person() = runTest {
        val original = Persona(patente = "DEMO01", nombreApellido = "Ana", departamento = "A", descripcionAuto = "-")
        db.personaDao().insertIfAbsent(original)
        db.personaDao().insertIfAbsent(original.copy(nombreApellido = "Otro"))

        assertEquals("Ana", db.personaDao().getByPatente("DEMO01")?.nombreApellido)
    }

    @Test
    fun question_bank_insert_is_idempotent() = runTest {
        val q = ExamenPregunta(pregunta = "P1", opcionA = "a", opcionB = "b", opcionC = "c", opcionD = "d", respuestaCorrecta = 0)
        db.examenDao().insertPreguntas(listOf(q, q))
        db.examenDao().insertPreguntas(listOf(q))
        assertEquals(1, db.examenDao().countPreguntas())
    }

    @Test
    fun seeder_loads_demo_data_once_on_fresh_install() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val seeder = DatabaseSeeder(database = { db }, resources = context.resources)
        seeder.onDatabaseCreated()

        seeder.seedIfNeeded()
        val personas = db.personaDao().count()
        val preguntas = db.examenDao().countPreguntas()
        seeder.seedIfNeeded()

        assertTrue(personas > 0)
        assertTrue(preguntas > 0)
        assertEquals(personas, db.personaDao().count())
        assertEquals(preguntas, db.examenDao().countPreguntas())
        assertTrue("seed must use PersonaType codes", db.personaDao().getAll().all { it.tipoPersona == it.tipoPersona?.uppercase() })
    }
}
