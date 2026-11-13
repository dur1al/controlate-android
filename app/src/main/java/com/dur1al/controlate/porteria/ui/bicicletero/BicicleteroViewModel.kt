package com.dur1al.controlate.porteria.ui.bicicletero

import androidx.lifecycle.*
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.repository.BicicletaRepository
import kotlinx.coroutines.launch

class BicicleteroViewModel(private val repository: BicicletaRepository) : ViewModel() {

    val todosLosRegistros: LiveData<List<RegistroBicicleta>> =
        repository.getAllRegistros().asLiveData()

    fun getRegistrosByShift(shiftId: Long): LiveData<List<RegistroBicicleta>> =
        repository.getRegistrosByShift(shiftId).asLiveData()

    fun registrarEntrada(registro: RegistroBicicleta) {
        viewModelScope.launch {
            repository.insert(registro)
        }
    }

    fun actualizarRegistro(registro: RegistroBicicleta) {
        viewModelScope.launch {
            repository.update(registro)
        }
    }
}


