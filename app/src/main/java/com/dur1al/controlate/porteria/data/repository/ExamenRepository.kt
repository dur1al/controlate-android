package com.dur1al.controlate.porteria.data.repository

import com.dur1al.controlate.porteria.data.local.dao.ExamenDao
import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.data.local.entities.ExamenResultado
import kotlinx.coroutines.flow.Flow

class ExamenRepository(private val examenDao: ExamenDao) {

    suspend fun getRandomPreguntas(limit: Int = 60): List<ExamenPregunta> =
        examenDao.getRandomPreguntas(limit)

    suspend fun getAllPreguntas(): List<ExamenPregunta> =
        examenDao.getAllPreguntas()

    suspend fun getPreguntasCount(): Int =
        examenDao.getPreguntasCount()

    suspend fun insertPreguntas(preguntas: List<ExamenPregunta>) =
        examenDao.insertPreguntas(preguntas)

    suspend fun insertResultado(resultado: ExamenResultado): Long =
        examenDao.insertResultado(resultado)

    fun getAllResultados(): Flow<List<ExamenResultado>> =
        examenDao.getAllResultados()

    suspend fun getUltimosResultados(): List<ExamenResultado> =
        examenDao.getUltimosResultados()
}
