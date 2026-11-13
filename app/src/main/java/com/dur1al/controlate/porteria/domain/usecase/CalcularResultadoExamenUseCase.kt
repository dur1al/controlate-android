package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.data.local.entities.ExamenResultado
import com.dur1al.controlate.porteria.data.repository.ExamenRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalcularResultadoExamenUseCase(
    private val repository: ExamenRepository
) {
    suspend operator fun invoke(
        guardiaNombre: String,
        preguntas: List<ExamenPregunta>,
        respuestasUsuario: Map<Int, Int>, // IndicePregunta -> OpcionSeleccionada (0..3)
        tiempoEmpleadoSegundos: Long
    ): ExamenResultado {
        val total = preguntas.size.coerceAtLeast(1)
        var correctas = 0

        preguntas.forEachIndexed { index, pregunta ->
            val seleccion = respuestasUsuario[index]
            if (seleccion != null && seleccion == pregunta.respuestaCorrecta) {
                correctas++
            }
        }

        val porcentaje = (correctas.toFloat() / total.toFloat()) * 100f
        val aprobado = porcentaje >= PORCENTAJE_MINIMO_APROBACION

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fechaStr = dateFormat.format(Date())

        val resultado = ExamenResultado(
            guardiaNombre = guardiaNombre.ifBlank { "Guardia OS-10" },
            fecha = fechaStr,
            puntaje = correctas,
            totalPreguntas = total,
            porcentaje = porcentaje,
            aprobado = aprobado,
            tiempoEmpleadoSegundos = tiempoEmpleadoSegundos
        )

        val idInsertado = repository.insertResultado(resultado)
        return resultado.copy(id = idInsertado)
    }

    companion object {
        const val PORCENTAJE_MINIMO_APROBACION = 70.0f
        const val MINIMO_CORRECTAS_APROBACION_60 = 42
    }
}
