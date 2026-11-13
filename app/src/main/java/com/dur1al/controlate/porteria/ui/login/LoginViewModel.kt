package com.dur1al.controlate.porteria.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.local.security.AdminPinManager
import com.dur1al.controlate.porteria.data.local.security.AdminPinManager.VerifyResult
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.ShiftType
import com.dur1al.controlate.porteria.domain.usecase.GetActiveShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.StartShiftUseCase
import com.dur1al.controlate.porteria.ui.common.EventChannel
import com.dur1al.controlate.porteria.ui.common.UiMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Inicio de turno y acceso de administrador.
 *
 * La verificación del PIN (PBKDF2, costosa) corre en [cpuDispatcher] para no bloquear la UI.
 */
class LoginViewModel(
    private val getActiveShift: GetActiveShiftUseCase,
    private val startShift: StartShiftUseCase,
    private val pins: AdminPinManager,
    private val cpuDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    data class UiState(
        val checkingSession: Boolean = true,
        val kind: ControlPoint.Kind = ControlPoint.Kind.PORTERIA,
        val points: List<ControlPoint> = ControlPoint.ofKind(ControlPoint.Kind.PORTERIA),
        val selectedPoint: ControlPoint = points.first(),
        val shiftType: ShiftType = ShiftType.DAY,
        val guardNameError: Boolean = false,
        val busy: Boolean = false
    )

    sealed interface Event {
        data class OpenShift(val point: ControlPoint) : Event
        data object OpenAdmin : Event
        data object AskPin : Event
        data object AskPinSetup : Event
        data class Message(val message: UiMessage) : Event
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val eventChannel = EventChannel<Event>()
    val events: Flow<Event> = eventChannel.events

    init {
        viewModelScope.launch {
            val active = getActiveShift()
            if (active != null) {
                eventChannel.send(Event.OpenShift(ControlPoint.fromId(active.controlPoint) ?: ControlPoint.PORTERIA_2))
            } else {
                _state.update { it.copy(checkingSession = false) }
            }
        }
    }

    fun selectKind(kind: ControlPoint.Kind) {
        val points = ControlPoint.ofKind(kind)
        _state.update { it.copy(kind = kind, points = points, selectedPoint = points.first()) }
    }

    fun selectPoint(point: ControlPoint) = _state.update { it.copy(selectedPoint = point) }

    fun selectShiftType(type: ShiftType) = _state.update { it.copy(shiftType = type) }

    fun clearGuardNameError() = _state.update { it.copy(guardNameError = false) }

    fun startShift(guardName: String) {
        val current = _state.value
        if (current.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (startShift(guardName, current.selectedPoint, current.shiftType)) {
                is StartShiftUseCase.Outcome.Started ->
                    eventChannel.send(Event.OpenShift(current.selectedPoint))
                StartShiftUseCase.Outcome.InvalidGuardName ->
                    _state.update { it.copy(guardNameError = true) }
            }
            _state.update { it.copy(busy = false) }
        }
    }

    fun onAdminClicked() {
        when {
            !pins.isPinConfigured() -> eventChannel.send(Event.AskPinSetup)
            pins.remainingLockoutSeconds() > 0 ->
                eventChannel.send(lockedOut(pins.remainingLockoutSeconds()))
            else -> eventChannel.send(Event.AskPin)
        }
    }

    fun submitPin(pin: String) {
        viewModelScope.launch {
            val result = withContext(cpuDispatcher) { pins.verify(pin) }
            eventChannel.send(
                when (result) {
                    VerifyResult.Granted -> Event.OpenAdmin
                    is VerifyResult.Denied -> Event.Message(UiMessage(R.string.admin_pin_wrong_attempts, listOf(result.remainingAttempts)))
                    is VerifyResult.LockedOut -> lockedOut(result.remainingSeconds)
                    VerifyResult.NotConfigured -> Event.AskPinSetup
                }
            )
        }
    }

    fun setupPin(pin: String, confirmation: String) {
        when {
            pin != confirmation -> eventChannel.send(Event.Message(UiMessage(R.string.login_admin_pin_mismatch)))
            !pins.isValidFormat(pin) -> eventChannel.send(Event.Message(UiMessage(R.string.login_admin_pin_invalid_format)))
            else -> viewModelScope.launch {
                withContext(cpuDispatcher) { pins.setPin(pin) }
                eventChannel.send(Event.OpenAdmin)
            }
        }
    }

    private fun lockedOut(seconds: Long) = Event.Message(UiMessage(R.string.login_admin_locked_out, listOf(seconds)))
}
