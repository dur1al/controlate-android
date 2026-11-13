package com.dur1al.controlate

import com.dur1al.controlate.porteria.data.local.dao.ExamenDao
import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.data.local.entities.ExamenResultado
import com.dur1al.controlate.porteria.data.repository.ExamenRepository
import com.dur1al.controlate.porteria.domain.usecase.CalcularResultadoExamenUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalcularResultadoExamenUseCaseTest {

    private lateinit var fakeRepository: ExamenRepository
    private lateinit var useCase: CalcularResultadoExamenUseCase

    @Before
    fun setUp() {
        fakeRepository = ExamenRepository(
            object : ExamenDao {
                override suspend fun insertPregunta(pregunta: ExamenPregunta) {}
                override suspend fun insertPreguntas(preguntas: List<ExamenPregunta>) {}
                override suspend fun getRandomPreguntas(limit: Int): List<ExamenPregunta> = emptyList()
                override suspend fun getAllPreguntas(): List<ExamenPregunta> = emptyList()
                override suspend fun getPreguntasCount(): Int = 0
                override suspend fun deleteAllPreguntas() {}
                override suspend fun insertResultado(resultado: ExamenResultado): Long = 1L
                override fun getAllResultados(): Flow<List<ExamenResultado>> = emptyFlow()
                override suspend fun getUltimosResultados(): List<ExamenResultado> = emptyList()
            }
        )
        useCase = CalcularResultadoExamenUseCase(fakeRepository)
    }

    @Test
    fun `cuando responde 42 de 60 correctamente debe aprobar con 70 por ciento`() = runBlocking {
        val preguntas = crear60PreguntasFicticias()
        // Responder correctamente las primeras 42 preguntas (0..41)
        val respuestasUsuario = mutableMapOf<Int, Int>()
        for (i in 0 until 42) {
            respuestasUsuario[i] = 1 // Respuesta correcta esperada es 1
        }
        for (i in 42 until 60) {
            respuestasUsuario[i] = 0 // Respuesta incorrecta (esperada es 1)
        }

        val resultado = useCase(
            guardiaNombre = "Guardia Test",
            preguntas = preguntas,
            respuestasUsuario = respuestasUsuario,
            tiempoEmpleadoSegundos = 1200L
        )

        assertEquals(42, resultado.puntaje)
        assertEquals(60, resultado.totalPreguntas)
        assertEquals(70.0f, resultado.porcentaje, 0.01f)
        assertTrue(resultado.aprobado)
    }

    @Test
    fun `cuando responde 41 de 60 correctamente debe reprobar`() = runBlocking {
        val preguntas = crear60PreguntasFicticias()
        val respuestasUsuario = mutableMapOf<Int, Int>()
        for (i in 0 until 41) {
            respuestasUsuario[i] = 1
        }

        val resultado = useCase(
            guardiaNombre = "Guardia Test",
            preguntas = preguntas,
            respuestasUsuario = respuestasUsuario,
            tiempoEmpleadoSegundos = 1800L
        )

        assertEquals(41, resultado.puntaje)
        assertFalse(resultado.aprobado)
    }

    @Test
    fun `cuando responde 60 de 60 correctamente debe obtener 100 por ciento`() = runBlocking {
        val preguntas = crear60PreguntasFicticias()
        val respuestasUsuario = (0 until 60).associateWith { 1 }

        val resultado = useCase(
            guardiaNombre = "Guardia Experto",
            preguntas = preguntas,
            respuestasUsuario = respuestasUsuario,
            tiempoEmpleadoSegundos = 900L
        )

        assertEquals(60, resultado.puntaje)
        assertEquals(100.0f, resultado.porcentaje, 0.01f)
        assertTrue(resultado.aprobado)
    }

    private fun crear60PreguntasFicticias(): List<ExamenPregunta> {
        return (1..60).map { i ->
            ExamenPregunta(
                id = i,
                pregunta = "Pregunta Ficticia $i",
                opcionA = "Opcion A",
                opcionB = "Opcion B (Correcta)",
                opcionC = "Opcion C",
                opcionD = "Opcion D",
                respuestaCorrecta = 1, // B
                categoria = "Prueba Unitarias"
            )
        }
    }
}
