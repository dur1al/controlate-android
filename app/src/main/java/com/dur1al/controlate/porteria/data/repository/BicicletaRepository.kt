package com.dur1al.controlate.porteria.data.repository

import com.dur1al.controlate.porteria.data.local.dao.BicicletaDao
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio dedicado a operaciones de registros de bicicletas.
 * Separado del RegistroRepository original para Single Responsibility.
 */
class BicicletaRepository(private val bicicletaDao: BicicletaDao) {

    fun getAllRegistros(): Flow<List<RegistroBicicleta>> = bicicletaDao.getAllRegistros()

    fun getRegistrosByShift(shiftId: Long): Flow<List<RegistroBicicleta>> =
        bicicletaDao.getRegistrosByShift(shiftId)

    suspend fun insert(registro: RegistroBicicleta) = bicicletaDao.insert(registro)

    suspend fun update(registro: RegistroBicicleta) = bicicletaDao.update(registro)

    suspend fun getUltimoPorRut(rut: String): RegistroBicicleta? =
        bicicletaDao.getUltimoRegistroPorRut(rut)

    suspend fun getRecordsByDate(date: String): List<RegistroBicicleta> =
        bicicletaDao.getRecordsByDateList(date)

    suspend fun getRecordsByDateAndPoint(date: String, point: String): List<RegistroBicicleta> =
        bicicletaDao.getRecordsByDateAndPointList(date, point)

    suspend fun getRecordsByShiftList(shiftId: Long): List<RegistroBicicleta> =
        bicicletaDao.getRecordsByShiftList(shiftId)

    suspend fun deleteByShift(shiftId: Long) = bicicletaDao.deleteByShift(shiftId)

    suspend fun deleteAll() = bicicletaDao.deleteAll()

    // region Stats

    suspend fun countByFecha(fecha: String): Int = bicicletaDao.countRegistrosByFecha(fecha)

    suspend fun countSalidasByFecha(fecha: String): Int = bicicletaDao.countSalidasByFecha(fecha)

    suspend fun countByFechaAndPoint(fecha: String, point: String): Int =
        bicicletaDao.countRegistrosByFechaAndPoint(fecha, point)

    suspend fun countSalidasByFechaAndPoint(fecha: String, point: String): Int =
        bicicletaDao.countSalidasByFechaAndPoint(fecha, point)

    val allShiftSummaries: Flow<List<BicicletaDao.BicicletaShiftSummary>> =
        bicicletaDao.getAllShiftSummaries()

    // endregion
}
