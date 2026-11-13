package com.dur1al.controlate.porteria.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.local.security.AdminPinManager
import com.dur1al.controlate.porteria.data.local.security.AdminPinManager.ChangeResult
import com.dur1al.controlate.porteria.ui.common.EventChannel
import com.dur1al.controlate.porteria.ui.common.UiMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Cambio del PIN de administrador. */
class SettingsViewModel(
    private val pins: AdminPinManager,
    private val cpuDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    sealed interface Event {
        data object PinChanged : Event
        data object WrongCurrentPin : Event
        data object InvalidNewPin : Event
        data class Message(val message: UiMessage) : Event
    }

    private val eventChannel = EventChannel<Event>()
    val events: Flow<Event> = eventChannel.events

    fun changePin(currentPin: String, newPin: String) {
        if (newPin.isBlank() || (pins.isPinConfigured() && currentPin.isBlank())) {
            return eventChannel.send(Event.Message(UiMessage(R.string.settings_pin_fill_fields)))
        }
        viewModelScope.launch {
            val result = withContext(cpuDispatcher) { pins.changePin(currentPin, newPin) }
            eventChannel.send(
                when (result) {
                    ChangeResult.Changed -> Event.PinChanged
                    ChangeResult.WrongCurrentPin -> Event.WrongCurrentPin
                    ChangeResult.InvalidNewPin -> Event.InvalidNewPin
                    ChangeResult.LockedOut -> Event.Message(
                        UiMessage(R.string.login_admin_locked_out, listOf(pins.remainingLockoutSeconds()))
                    )
                }
            )
        }
    }
}
