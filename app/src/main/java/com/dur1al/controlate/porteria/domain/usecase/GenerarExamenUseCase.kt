package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.data.repository.ExamenRepository

class GenerarExamenUseCase(
    private val repository: ExamenRepository
) {
    suspend operator fun invoke(cantidad: Int = CANTIDAD_OFICIAL_PREGUNTAS): List<ExamenPregunta> {
        val preguntas = repository.getRandomPreguntas(cantidad)
        return if (preguntas.size > cantidad) {
            preguntas.take(cantidad)
        } else {
            preguntas
        }
    }

    companion object {
        const val CANTIDAD_OFICIAL_PREGUNTAS = 60
    }
}
