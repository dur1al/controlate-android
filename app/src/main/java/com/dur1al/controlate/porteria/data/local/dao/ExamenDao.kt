package com.dur1al.controlate.porteria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.data.local.entities.ExamenResultado
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamenDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPregunta(pregunta: ExamenPregunta)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreguntas(preguntas: List<ExamenPregunta>)

    @Query("SELECT * FROM examen_preguntas ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomPreguntas(limit: Int = 60): List<ExamenPregunta>

    @Query("SELECT * FROM examen_preguntas ORDER BY id ASC")
    suspend fun getAllPreguntas(): List<ExamenPregunta>

    @Query("SELECT COUNT(*) FROM examen_preguntas")
    suspend fun getPreguntasCount(): Int

    @Query("DELETE FROM examen_preguntas")
    suspend fun deleteAllPreguntas()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResultado(resultado: ExamenResultado): Long

    @Query("SELECT * FROM examen_resultados ORDER BY id DESC")
    fun getAllResultados(): Flow<List<ExamenResultado>>

    @Query("SELECT * FROM examen_resultados ORDER BY id DESC LIMIT 10")
    suspend fun getUltimosResultados(): List<ExamenResultado>
}
