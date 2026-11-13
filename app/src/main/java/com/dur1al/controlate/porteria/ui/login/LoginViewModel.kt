package com.dur1al.controlate.porteria.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.local.security.AdminPinManager
import com.dur1al.controlate.porteria.data.local.security.AdminPinManager.VerifyResult
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.ShiftType
import com.dur1al.controlate.porteria.domain.repository.ControlPointRepository
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
    private val controlPoints: ControlPointRepository,
    private val cpuDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    data class UiState(
        val checkingSession: Boolean = true,
        val kind: ControlPoint.Kind = ControlPoint.Kind.PORTERIA,
        /** Puntos activos del tipo elegido, en el orden definido por el administrador. */
        val points: List<ControlPoint> = emptyList(),
        /** `null` si el tipo elegido no tiene puntos activos. */
        val selectedPoint: ControlPoint? = null,
        val shiftType: ShiftType = ShiftType.DAY,
        val guardNameError: Boolean = false,
        val busy: Boolean = false
    )

    sealed interface Event {
        data class OpenShift(val kind: ControlPoint.Kind) : Event
        data object OpenAdmin : Event
        data object AskPin : Event
        data object AskPinSetup : Event
        data class Message(val message: UiMessage) : Event
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val eventChannel = EventChannel<Event>()
    val events: Flow<Event> = eventChannel.events

    /** Puntos activos de ambos tipos; se observa para reflejar cambios hechos en administración. */
    private var activePoints: List<ControlPoint> = emptyList()

    init {
        viewModelScope.launch {
            val active = getActiveShift()
            if (active != null) {
                val kind = controlPoints.get(active.controlPoint)?.kind
                    ?: ControlPoint.Kind.fromId(active.controlPoint)
                    ?: ControlPoint.Kind.PORTERIA
                eventChannel.send(Event.OpenShift(kind))
            } else {
                _state.update { it.copy(checkingSession = false) }
            }
        }
        viewModelScope.launch {
            controlPoints.observeAll().collect { all ->
                activePoints = all.filter { it.active }
                _state.update { it.withPointsOf(it.kind) }
            }
        }
    }

    fun selectKind(kind: ControlPoint.Kind) = _state.update { it.withPointsOf(kind) }

    /** Recalcula la lista del tipo y conserva la selección si sigue disponible. */
    private fun UiState.withPointsOf(kind: ControlPoint.Kind): UiState {
        val points = activePoints.filter { it.kind == kind }
        val keep = selectedPoint?.takeIf { s -> s.kind == kind && points.any { it.id == s.id } }
        return copy(kind = kind, points = points, selectedPoint = keep?.let { s -> points.first { it.id == s.id } } ?: points.firstOrNull())
    }

    fun selectPoint(point: ControlPoint) = _state.update { it.copy(selectedPoint = point) }

    fun selectShiftType(type: ShiftType) = _state.update { it.copy(shiftType = type) }

    fun clearGuardNameError() = _state.update { it.copy(guardNameError = false) }

    fun startShift(guardName: String) {
        val current = _state.value
        if (current.busy) return
        val point = current.selectedPoint ?: return eventChannel.send(Event.Message(UiMessage(R.string.login_no_active_points)))
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (startShift(guardName, point, current.shiftType)) {
                is StartShiftUseCase.Outcome.Started ->
                    eventChannel.send(Event.OpenShift(point.kind))
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
