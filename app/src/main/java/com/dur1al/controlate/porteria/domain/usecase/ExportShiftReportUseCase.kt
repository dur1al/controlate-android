package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.repository.RegistroRepository
import com.dur1al.controlate.porteria.domain.model.Result
import com.dur1al.controlate.porteria.utils.ExportResult
import com.dur1al.controlate.porteria.utils.ReportManager

/**
 * Encapsulates the logic for exporting full shift reports
 * or daily data using the ReportManager.
 */
class ExportShiftReportUseCase(
    private val registroRepository: RegistroRepository,
    private val bicicletaRepository: com.dur1al.controlate.porteria.data.repository.BicicletaRepository,
    private val reportManager: ReportManager
) {
    suspend fun exportPorteriaShift(
        shiftId: Long,
        guardiaName: String,
        porteriaName: String,
        fechaTurno: String,
        turnoTipo: String
    ): Result<ExportResult> {
        return try {
            val registros = registroRepository.getRecordsByShiftList(shiftId)
            val result = reportManager.generateAndShareExcel(
                registros = registros,
                guardiaName = guardiaName,
                porteriaName = porteriaName,
                fechaTurno = fechaTurno,
                turnoTipo = turnoTipo
            )
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error("Error exporting entry shift", e)
        }
    }

    suspend fun exportBicicletaShift(
        shiftId: Long,
        guardiaName: String,
        turnoTipo: String,
        shareAfterSave: Boolean = true
    ): Result<ExportResult> {
        return try {
            val registros = bicicletaRepository.getRecordsByShiftList(shiftId)
            val result = reportManager.generateBicicletaReportExcel(
                registros = registros,
                guardiaName = guardiaName,
                turnoTipo = turnoTipo,
                shareAfterSave = shareAfterSave
            )
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error("Error exporting bike shift", e)
        }
    }
}
