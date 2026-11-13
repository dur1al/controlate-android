package com.dur1al.controlate.porteria.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.porteria.data.repository.RegistroRepository
import com.dur1al.controlate.porteria.domain.model.Result
import com.dur1al.controlate.porteria.domain.util.DateFormatter
import com.dur1al.controlate.porteria.utils.ExportResult
import com.dur1al.controlate.porteria.utils.ReportManager
import kotlinx.coroutines.launch

class AdminViewModel(
    private val registroRepository: RegistroRepository,
    private val reportManager: ReportManager
) : ViewModel() {

    private val _stats = MutableLiveData<AdminStats>()
    val stats: LiveData<AdminStats> = _stats

    private val _recentLogs = MutableLiveData<List<com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso>>()
    val recentLogs: LiveData<List<com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso>> = _recentLogs

    private val _exportResult = MutableLiveData<ExportResult?>()
    val exportResult: LiveData<ExportResult?> = _exportResult

    fun loadStats() {
        viewModelScope.launch {
            val today = DateFormatter.todayDbFormat()
            val total = registroRepository.getCountRegistrosByFecha(today)
            val salidas = registroRepository.getCountSalidasByFecha(today)

            _stats.postValue(AdminStats(total, salidas, total - salidas))

            val logs = registroRepository.getRecordsByDate(today)
            _recentLogs.postValue(logs.sortedByDescending { it.id })
        }
    }

    fun exportReport() {
        viewModelScope.launch {
            _exportResult.value = null // reset
            try {
                val registros = registroRepository.getAllRecords()
                val result = reportManager.generateAndShareExcel(registros, "Admin", "General", "Hoy", "Admin")
                _exportResult.postValue(result)
            } catch (e: Exception) {
                _exportResult.postValue(ExportResult.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    data class AdminStats(val ingresos: Int, val salidas: Int, val enRecinto: Int)
}


