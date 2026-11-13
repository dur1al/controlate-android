package com.dur1al.controlate.porteria.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Pregunta del simulador OS-10. El índice único sobre [pregunta] hace idempotente la
 * carga del banco de preguntas (`INSERT OR IGNORE`).
 *
 * @property respuestaCorrecta índice de la opción correcta: 0 = A, 1 = B, 2 = C, 3 = D.
 */
@Entity(tableName = "examen_preguntas", indices = [Index(value = ["pregunta"], unique = true)])
data class ExamenPregunta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pregunta: String,
    val opcionA: String,
    val opcionB: String,
    val opcionC: String,
    val opcionD: String,
    val respuestaCorrecta: Int,
    val categoria: String = "General OS-10",
    val explicacion: String = ""
)
