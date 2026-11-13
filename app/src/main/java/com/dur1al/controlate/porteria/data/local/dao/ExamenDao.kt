package com.dur1al.controlate.porteria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.data.local.entities.ExamenResultado

@Dao
interface ExamenDao {

    /** Idempotente gracias al índice único sobre `pregunta`. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPreguntas(preguntas: List<ExamenPregunta>)

    @Query("SELECT * FROM examen_preguntas ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomPreguntas(limit: Int): List<ExamenPregunta>

    @Query("SELECT COUNT(*) FROM examen_preguntas")
    suspend fun countPreguntas(): Int

    @Insert
    suspend fun insertResultado(resultado: ExamenResultado): Long
}
