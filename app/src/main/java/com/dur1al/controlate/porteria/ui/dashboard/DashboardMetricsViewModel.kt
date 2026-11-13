package com.dur1al.controlate.porteria.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.dur1al.controlate.porteria.data.local.dao.RegistroDao
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import kotlinx.coroutines.flow.map

class DashboardMetricsViewModel(private val registroDao: RegistroDao) : ViewModel() {

    fun getMetrics(shiftId: Long): LiveData<Pair<Int, Int>> {
        return registroDao.getRegistrosByShift(shiftId)
            .map { registros: List<RegistroIngreso> ->
                val dentro = registros.count { it.horaSalida == null }
                val salieron = registros.count { it.horaSalida != null }
                Pair(dentro, salieron)
            }.asLiveData()
    }
}
