package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.testing.FakeClock
import com.dur1al.controlate.porteria.testing.FakeExamRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamUseCasesTest {

    private val clock = FakeClock()
    private val repository = FakeExamRepository(questions(70))
    private val calcular = CalcularResultadoExamenUseCase(repository, clock)

    private fun questions(n: Int) = (1..n).map {
        ExamenPregunta(id = it, pregunta = "P$it", opcionA = "a", opcionB = "b", opcionC = "c", opcionD = "d", respuestaCorrecta = 1)
    }

    @Test
    fun `generar returns at most the official number of questions`() = runTest {
        assertEquals(60, GenerarExamenUseCase(repository)().size)
    }

    @Test
    fun `42 of 60 correct passes with 70 percent`() = runTest {
        val preguntas = questions(60)
        val respuestas = (0 until 60).associateWith { if (it < 42) 1 else 0 }

        val r = calcular("Guardia", preguntas, respuestas, 1200)

        assertEquals(42, r.puntaje)
        assertEquals(60, r.totalPreguntas)
        assertEquals(70f, r.porcentaje, 0.01f)
        assertTrue(r.aprobado)
        assertEquals(clock.nowMs, r.realizadoAt)
        assertEquals(r, repository.saved.single().copy(id = r.id))
    }

    @Test
    fun `41 of 60 correct fails and unanswered questions count as wrong`() = runTest {
        val r = calcular("Guardia", questions(60), (0 until 41).associateWith { 1 }, 1800)
        assertEquals(41, r.puntaje)
        assertFalse(r.aprobado)
    }

    @Test
    fun `blank guard name is stored with a placeholder`() = runTest {
        val r = calcular(" ", questions(1), mapOf(0 to 1), 10)
        assertEquals(CalcularResultadoExamenUseCase.ANONYMOUS_GUARD, r.guardiaNombre)
    }

    @Test
    fun `empty exam scores zero instead of dividing by zero`() = runTest {
        val r = calcular("G", emptyList(), emptyMap(), 0)
        assertEquals(0f, r.porcentaje, 0f)
        assertFalse(r.aprobado)
    }
}
