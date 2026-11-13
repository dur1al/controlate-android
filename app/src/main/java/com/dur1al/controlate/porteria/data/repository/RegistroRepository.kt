package com.dur1al.controlate.porteria.data.repository

import com.dur1al.controlate.porteria.data.local.dao.BicicletaDao
import com.dur1al.controlate.porteria.data.local.dao.PersonaDao
import com.dur1al.controlate.porteria.data.local.dao.RegistroDao
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.domain.util.DateFormatter
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio principal que orquesta operaciones de registros de ingreso.
 *
 * Mantiene acceso a PersonaDao y BicicletaDao por compatibilidad con código
 * existente. Las operaciones de personas y bicicletas deben migrarse
 * a [PersonaRepository] y [BicicletaRepository] respectivamente.
 */
class RegistroRepository(
    private val registroDao: RegistroDao
) {

    // region Registros Ingreso

    fun getRegistrosByShift(shiftId: Long): Flow<List<RegistroIngreso>> =
        registroDao.getRegistrosByShift(shiftId)

    fun searchRegistrosByShift(query: String, shiftId: Long): Flow<List<RegistroIngreso>> =
        registroDao.searchRegistrosByShift(query, shiftId)

    suspend fun insertRegistro(registro: RegistroIngreso) {
        val finalRegistro = if (registro.horaEntrada.isEmpty()) {
            registro.copy(
                horaEntrada = DateFormatter.nowTime(),
                fechaEntrada = DateFormatter.todayDbFormat()
            )
        } else registro

        registroDao.insert(finalRegistro)
    }

    suspend fun updateRegistro(registro: RegistroIngreso) = registroDao.update(registro)

    suspend fun deleteRegistro(registro: RegistroIngreso) = registroDao.delete(registro)

    suspend fun markExit(registro: RegistroIngreso) {
        registroDao.update(registro.copy(horaSalida = DateFormatter.nowTime()))
    }

    suspend fun getRecordsByDate(date: String): List<RegistroIngreso> =
        registroDao.getRecordsByDateList(date)

    suspend fun getAllRecords(): List<RegistroIngreso> = registroDao.getAllRecordsList()

    suspend fun getRecordsByShiftList(shiftId: Long): List<RegistroIngreso> =
        registroDao.getRecordsByShiftList(shiftId)

    suspend fun deletePorteriaShift(shiftId: Long) = registroDao.deleteByShift(shiftId)

    // endregion

    // region Stats

    suspend fun getCountRegistrosByFecha(fecha: String) =
        registroDao.countRegistrosByFecha(fecha)

    suspend fun getCountSalidasByFecha(fecha: String) =
        registroDao.countSalidasByFecha(fecha)

    suspend fun getCountRegistrosByFechaAndPoint(fecha: String, point: String) =
        registroDao.countRegistrosByFechaAndPoint(fecha, point)

    suspend fun getCountSalidasByFechaAndPoint(fecha: String, point: String) =
        registroDao.countSalidasByFechaAndPoint(fecha, point)

    val allShifts: Flow<List<RegistroDao.ShiftSummary>> = registroDao.getAllShifts()

    // endregion

    suspend fun clearAllData() {
        registroDao.deleteAll()
    }
}

