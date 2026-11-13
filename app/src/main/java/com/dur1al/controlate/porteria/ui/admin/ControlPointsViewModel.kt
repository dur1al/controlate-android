package com.dur1al.controlate.porteria.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.repository.ControlPointRepository
import com.dur1al.controlate.porteria.domain.usecase.AddControlPointUseCase
import com.dur1al.controlate.porteria.domain.usecase.ControlPointNameError
import com.dur1al.controlate.porteria.domain.usecase.RenameControlPointUseCase
import com.dur1al.controlate.porteria.domain.usecase.SetControlPointActiveUseCase
import com.dur1al.controlate.porteria.ui.common.EventChannel
import com.dur1al.controlate.porteria.ui.common.UiMessage
import com.dur1al.controlate.porteria.ui.common.runCatchingCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Administración de puntos de control: agregar, renombrar y activar o desactivar. */
class ControlPointsViewModel(
    repository: ControlPointRepository,
    private val addPoint: AddControlPointUseCase,
    private val renamePoint: RenameControlPointUseCase,
    private val setActive: SetControlPointActiveUseCase
) : ViewModel() {

    data class UiState(val loading: Boolean = true, val points: List<ControlPoint> = emptyList()) {
        /** Número sugerido para el nombre por defecto de un punto nuevo ("Portería 4"). */
        fun nextNumber(kind: ControlPoint.Kind): Int = points.count { it.kind == kind } + 1
    }

    sealed interface Event {
        data class Message(val message: UiMessage) : Event

        /** El diálogo de alta o renombre terminó bien y puede cerrarse. */
        data object DialogDone : Event
    }

    val state: StateFlow<UiState> = repository.observeAll()
        .map { UiState(loading = false, points = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState())

    /** Error del nombre en el diálogo abierto; mientras exista, el diálogo no se cierra. */
    private val _nameError = MutableStateFlow<ControlPointNameError?>(null)
    val nameError: StateFlow<ControlPointNameError?> = _nameError.asStateFlow()

    /** `true` mientras se guarda: evita altas duplicadas por doble toque. */
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val eventChannel = EventChannel<Event>()
    val events: Flow<Event> = eventChannel.events

    fun startDialog() {
        _nameError.value = null
    }

    fun clearNameError() {
        _nameError.value = null
    }

    fun add(kind: ControlPoint.Kind, name: String) = save {
        when (val outcome = addPoint(kind, name)) {
            is AddControlPointUseCase.Outcome.Added -> done(UiMessage(R.string.control_points_added, listOf(outcome.point.name)))
            is AddControlPointUseCase.Outcome.Invalid -> _nameError.value = outcome.error
        }
    }

    fun rename(id: String, name: String) = save {
        when (val outcome = renamePoint(id, name)) {
            is RenameControlPointUseCase.Outcome.Renamed -> done(UiMessage(R.string.control_points_renamed))
            is RenameControlPointUseCase.Outcome.Invalid -> _nameError.value = outcome.error
            RenameControlPointUseCase.Outcome.NotFound -> done(UiMessage(R.string.error_generic))
        }
    }

    fun setActive(id: String, active: Boolean) {
        viewModelScope.launch {
            val message = when (setActive.invoke(id, active)) {
                SetControlPointActiveUseCase.Outcome.Updated ->
                    if (active) R.string.control_points_activated else R.string.control_points_deactivated
                SetControlPointActiveUseCase.Outcome.LastActiveOfKind -> R.string.control_points_last_active
                SetControlPointActiveUseCase.Outcome.NotFound -> R.string.error_generic
            }
            eventChannel.send(Event.Message(UiMessage(message)))
        }
    }

    private fun save(block: suspend () -> Unit) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            runCatchingCancellable { block() }
                .onFailure { eventChannel.send(Event.Message(UiMessage(R.string.error_generic))) }
            _saving.value = false
        }
    }

    private fun done(message: UiMessage) {
        _nameError.value = null
        eventChannel.send(Event.DialogDone)
        eventChannel.send(Event.Message(message))
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
