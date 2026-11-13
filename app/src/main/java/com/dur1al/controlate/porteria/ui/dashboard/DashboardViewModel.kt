package com.dur1al.controlate.porteria.ui.dashboard

import androidx.lifecycle.*
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.data.repository.PersonaRepository
import com.dur1al.controlate.porteria.data.repository.RegistroRepository
import com.dur1al.controlate.porteria.domain.usecase.MarkExitUseCase
import com.dur1al.controlate.porteria.domain.usecase.SaveRegistroUseCase
import com.dur1al.controlate.porteria.domain.usecase.SearchPersonaUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val registroRepository: RegistroRepository,
    private val personaRepository: PersonaRepository,
    private val searchPersonaUseCase: SearchPersonaUseCase,
    private val saveRegistroUseCase: SaveRegistroUseCase,
    private val markExitUseCase: MarkExitUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _dbSearchQuery = MutableStateFlow("")
    private val _shiftId = MutableStateFlow(0L)

    val registrosToShow: LiveData<List<RegistroIngreso>> = _searchQuery
        .combine(_shiftId) { query, shiftId -> query to shiftId }
        .flatMapLatest { (query, shiftId) ->
            if (query.isBlank()) registroRepository.getRegistrosByShift(shiftId)
            else registroRepository.searchRegistrosByShift(query, shiftId)
        }.asLiveData()

    val filteredPersonas: LiveData<List<Persona>> = _dbSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) personaRepository.allPersonas
            else personaRepository.searchPersonas(query)
        }.asLiveData()

    val allPersonas: LiveData<List<Persona>> = personaRepository.allPersonas.asLiveData()

    private val _personaFound = MutableLiveData<Persona?>()
    val personaFound: LiveData<Persona?> = _personaFound

    fun searchByRut(rut: String) {
        viewModelScope.launch {
            _personaFound.value = searchPersonaUseCase.byRut(rut)
        }
    }

    fun clearPersonaFound() { _personaFound.value = null }

    suspend fun getPersonaByPlate(patente: String): Persona? = searchPersonaUseCase.byPatente(patente)

    private val _registroToEdit = MutableLiveData<RegistroIngreso?>(null)
    val registroToEdit: LiveData<RegistroIngreso?> = _registroToEdit

    fun setRegistroToEdit(registro: RegistroIngreso?) { _registroToEdit.value = registro }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateDbSearchQuery(query: String) { _dbSearchQuery.value = query }
    fun setShiftContext(shiftId: Long) { _shiftId.value = shiftId }

    fun saveEntry(
        porteria: String, porteriaName: String, guardia: String, fechaTurno: String, turnoTipo: String, shiftId: Long,
        patente: String, nombre: String, padron: String?, esPadron: Boolean, depto: String, rut: String?, autoDesc: String,
        empresa: String? = null, tipoPersona: String? = null, observaciones: String? = null
    ) {
        viewModelScope.launch {
            val shiftInfo = com.dur1al.controlate.porteria.domain.model.ShiftInfo(
                porteriaId = porteria,
                porteriaName = porteriaName,
                guardiaName = guardia,
                fechaTurno = fechaTurno,
                turnoTipo = turnoTipo,
                shiftId = shiftId
            )
            saveRegistroUseCase(
                shiftInfo = shiftInfo,
                patente = patente,
                nombre = nombre,
                padron = padron,
                esPadron = esPadron,
                departamento = depto,
                rut = rut,
                descripcionAuto = autoDesc,
                empresa = empresa,
                tipoPersona = tipoPersona,
                observaciones = observaciones
            )
        }
    }

    fun updatePersona(persona: Persona) { viewModelScope.launch { personaRepository.update(persona) } }
    fun deletePersona(persona: Persona) { viewModelScope.launch { personaRepository.delete(persona) } }
    fun markExit(registro: RegistroIngreso) { viewModelScope.launch { markExitUseCase(registro) } }
    fun deleteRegistro(registro: RegistroIngreso) { viewModelScope.launch { registroRepository.deleteRegistro(registro) } }
    fun importPersonas(personas: List<Persona>) { viewModelScope.launch { personaRepository.insertAll(personas) } }
    fun clearAllData() { viewModelScope.launch { registroRepository.clearAllData() } }
}



