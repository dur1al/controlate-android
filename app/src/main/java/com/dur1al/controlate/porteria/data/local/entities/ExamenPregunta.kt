package com.dur1al.controlate.porteria.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "examen_preguntas")
data class ExamenPregunta(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val pregunta: String,
    val opcionA: String,
    val opcionB: String,
    val opcionC: String,
    val opcionD: String,
    val respuestaCorrecta: Int, // 0: A, 1: B, 2: C, 3: D
    val categoria: String = "General OS-10",
    val explicacion: String = ""
)
