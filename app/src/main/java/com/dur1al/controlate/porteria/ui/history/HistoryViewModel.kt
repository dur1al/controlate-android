package com.dur1al.controlate.porteria.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.porteria.data.local.dao.RegistroDao
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de gestionar los datos históricos de registros.
 * Proporciona lógica para filtrar registros por turno.
 */
class HistoryViewModel(private val registroDao: RegistroDao) : ViewModel() {

    private val _registros = MutableStateFlow<List<RegistroIngreso>>(emptyList())
    val registros: StateFlow<List<RegistroIngreso>> = _registros

    /**
     * Carga registros filtrados por un ID de turno específico.
     */
    fun loadRegistrosByShift(shiftId: Long) {
        viewModelScope.launch {
            _registros.value = registroDao.getRecordsByShiftList(shiftId)
        }
    }
}
