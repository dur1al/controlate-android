package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.data.local.entities.ExamenResultado
import com.dur1al.controlate.porteria.domain.repository.ExamRepository
import com.dur1al.controlate.porteria.domain.time.Clock

/** Selecciona preguntas al azar del banco local. */
class GenerarExamenUseCase(private val repository: ExamRepository) {
    suspend operator fun invoke(cantidad: Int = CANTIDAD_OFICIAL_PREGUNTAS): List<ExamenPregunta> =
        repository.randomQuestions(cantidad).take(cantidad)

    companion object {
        const val CANTIDAD_OFICIAL_PREGUNTAS = 60
        const val DURACION_MINUTOS = 60
    }
}

/** Corrige un intento, calcula el porcentaje y persiste el resultado. */
class CalcularResultadoExamenUseCase(
    private val repository: ExamRepository,
    private val clock: Clock
) {
    /** @param respuestas índice de pregunta -> opción elegida (0..3). Sin respuesta cuenta como incorrecta. */
    suspend operator fun invoke(
        guardiaNombre: String,
        preguntas: List<ExamenPregunta>,
        respuestas: Map<Int, Int>,
        tiempoEmpleadoSegundos: Long
    ): ExamenResultado {
        val correctas = preguntas.indices.count { respuestas[it] == preguntas[it].respuestaCorrecta }
        val porcentaje = if (preguntas.isEmpty()) 0f else correctas * 100f / preguntas.size
        val resultado = ExamenResultado(
            guardiaNombre = guardiaNombre.trim().ifEmpty { ANONYMOUS_GUARD },
            realizadoAt = clock.now(),
            puntaje = correctas,
            totalPreguntas = preguntas.size,
            porcentaje = porcentaje,
            aprobado = preguntas.isNotEmpty() && porcentaje >= PORCENTAJE_APROBACION,
            tiempoEmpleadoSegundos = tiempoEmpleadoSegundos
        )
        return resultado.copy(id = repository.saveResult(resultado))
    }

    companion object {
        const val PORCENTAJE_APROBACION = 70f
        const val ANONYMOUS_GUARD = "Guardia OS-10"
    }
}
