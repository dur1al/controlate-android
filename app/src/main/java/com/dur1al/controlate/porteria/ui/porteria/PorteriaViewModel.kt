package com.dur1al.controlate.porteria.ui.porteria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.model.VehicleEntryInput
import com.dur1al.controlate.porteria.domain.report.ReportExporter
import com.dur1al.controlate.porteria.domain.repository.ControlPointRepository
import com.dur1al.controlate.porteria.domain.repository.VehicleEntryRepository
import com.dur1al.controlate.porteria.domain.usecase.BuildReportUseCase
import com.dur1al.controlate.porteria.domain.usecase.EndShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.FindPersonaUseCase
import com.dur1al.controlate.porteria.domain.usecase.GetActiveShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.MarkExitUseCase
import com.dur1al.controlate.porteria.domain.usecase.RegisterEntryOutcome
import com.dur1al.controlate.porteria.domain.usecase.RegisterVehicleEntryUseCase
import com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase
import com.dur1al.controlate.porteria.ui.common.EventChannel
import com.dur1al.controlate.porteria.ui.common.UiMessage
import com.dur1al.controlate.porteria.ui.common.runCatchingCancellable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Pantalla principal de portería vehicular: listado del turno, ingresos, salidas y cierre. */
@OptIn(ExperimentalCoroutinesApi::class)
class PorteriaViewModel(
    private val getActiveShift: GetActiveShiftUseCase,
    private val registerEntry: RegisterVehicleEntryUseCase,
    private val markExit: MarkExitUseCase,
    private val findPersona: FindPersonaUseCase,
    entries: VehicleEntryRepository,
    private val closeShift: EndShiftUseCase,
    private val buildReport: BuildReportUseCase,
    private val exporter: ReportExporter,
    private val validator: ValidateRegistroUseCase,
    private val controlPoints: ControlPointRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val turno: Turno? = null,
        /** Punto del turno (color y nombre); `null` si el id ya no existe en la tabla. */
        val point: ControlPoint? = null,
        val query: String = "",
        val registros: List<RegistroIngreso> = emptyList(),
        val busy: Boolean = false
    )

    sealed interface Event {
        data object NoActiveShift : Event
        data object ShiftEnded : Event
        data class ExitMarked(val id: Long, val at: Long, val patente: String) : Event
        data class Message(val message: UiMessage) : Event
    }

    /** Eventos del formulario de ingreso (los consume el bottom sheet, no la Activity). */
    sealed interface FormEvent {
        data object Saved : FormEvent
        data class Autofill(val persona: Persona) : FormEvent
        data class Failed(val message: UiMessage) : FormEvent
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _formErrors = MutableStateFlow<List<ValidationError>>(emptyList())
    val formErrors: StateFlow<List<ValidationError>> = _formErrors.asStateFlow()

    /** `true` mientras se guarda un ingreso: el formulario deshabilita "Registrar" (evita duplicados por doble toque). */
    private val _formSaving = MutableStateFlow(false)
    val formSaving: StateFlow<Boolean> = _formSaving.asStateFlow()

    /** Última persona autocompletada en el formulario abierto: evita repetir el aviso en cada búsqueda. */
    private var lastAutofillId: Int? = null

    private val eventChannel = EventChannel<Event>()
    val events: Flow<Event> = eventChannel.events

    private val formChannel = EventChannel<FormEvent>()
    val formEvents: Flow<FormEvent> = formChannel.events

    private val shiftId = MutableStateFlow<Long?>(null)
    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            val turno = getActiveShift()
            if (turno == null) {
                _state.update { it.copy(loading = false) }
                eventChannel.send(Event.NoActiveShift)
            } else {
                _state.update { it.copy(turno = turno, point = controlPoints.get(turno.controlPoint)) }
                shiftId.value = turno.id
            }
        }
        viewModelScope.launch {
            combine(shiftId.filterNotNull(), query) { id, q -> id to q }
                .flatMapLatest { (id, q) -> entries.observeByShift(id, q) }
                .collect { list -> _state.update { it.copy(loading = false, registros = list) } }
        }
    }

    fun setQuery(value: String) {
        query.value = value.trim()
        _state.update { it.copy(query = value) }
    }

    /** Se llama al abrir el formulario: parte sin errores ni autocompletado previo. */
    fun onFormOpened() {
        _formErrors.value = emptyList()
        lastAutofillId = null
    }

    /** Quita el error de un campo en cuanto el guardia lo corrige (el resto se mantiene visible). */
    fun clearFieldError(field: ValidationError.Field) {
        _formErrors.update { errors -> errors.filterNot { it.field == field } }
    }

    /**
     * Valida un campo al salir de él. Solo marca errores de formato: "obligatorio" se informa
     * al pulsar "Registrar", para no regañar al guardia por un campo que aún no completa.
     */
    fun onFieldBlur(field: ValidationError.Field, value: String) {
        if (value.isBlank()) return
        val error = when (field) {
            ValidationError.Field.PATENTE -> validator.patente(value)
            ValidationError.Field.NOMBRE -> validator.nombre(value)
            ValidationError.Field.RUT -> validator.rut(value, required = false)
            else -> null
        } ?: return
        _formErrors.update { errors -> errors.filterNot { it.field == field } + error }
    }

    fun register(input: VehicleEntryInput) {
        val id = shiftId.value ?: return eventChannel.send(Event.NoActiveShift)
        if (_formSaving.value) return
        _formSaving.value = true
        viewModelScope.launch {
            runCatchingCancellable { registerEntry(id, input) }
                .onSuccess { outcome ->
                    when (outcome) {
                        is RegisterEntryOutcome.Invalid -> _formErrors.value = outcome.errors
                        is RegisterEntryOutcome.Saved -> {
                            _formErrors.value = emptyList()
                            formChannel.send(FormEvent.Saved)
                            eventChannel.send(Event.Message(UiMessage(R.string.entry_registered)))
                        }
                    }
                }
                .onFailure { formChannel.send(FormEvent.Failed(UiMessage(R.string.error_save_entry))) }
            _formSaving.value = false
        }
    }

    fun lookupPlate(raw: String) {
        viewModelScope.launch { findPersona.byPatente(raw)?.let(::offerAutofill) }
    }

    fun lookupRut(raw: String) {
        viewModelScope.launch { findPersona.byRut(raw)?.let(::offerAutofill) }
    }

    private fun offerAutofill(persona: Persona) {
        if (persona.id == lastAutofillId) return
        lastAutofillId = persona.id
        formChannel.send(FormEvent.Autofill(persona))
    }

    fun markExit(registro: RegistroIngreso) {
        viewModelScope.launch {
            val at = markExit.vehicle(registro.id)
            eventChannel.send(
                if (at != null) Event.ExitMarked(registro.id, at, registro.patente)
                else Event.Message(UiMessage(R.string.exit_already_registered))
            )
        }
    }

    fun undoExit(id: Long, at: Long) {
        viewModelScope.launch {
            if (markExit.undoVehicle(id, at)) eventChannel.send(Event.Message(UiMessage(R.string.exit_undone)))
        }
    }

    /** Guarda el reporte del turno (para administración) y lo cierra. El cierre no depende del reporte. */
    fun endShift() {
        val id = shiftId.value ?: return eventChannel.send(Event.NoActiveShift)
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            runCatchingCancellable { buildReport.shift(id)?.let { exporter.export(it) } }
                .onFailure { eventChannel.send(Event.Message(UiMessage(R.string.error_generic))) }
            closeShift()
            _state.update { it.copy(busy = false) }
            eventChannel.send(Event.ShiftEnded)
        }
    }
}
