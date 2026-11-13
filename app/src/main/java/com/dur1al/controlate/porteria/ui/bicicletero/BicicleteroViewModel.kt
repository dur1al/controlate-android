package com.dur1al.controlate.porteria.ui.bicicletero

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.model.BikeEntryInput
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.report.ReportExporter
import com.dur1al.controlate.porteria.domain.repository.ControlPointRepository
import com.dur1al.controlate.porteria.domain.repository.BikeEntryRepository
import com.dur1al.controlate.porteria.domain.usecase.BuildReportUseCase
import com.dur1al.controlate.porteria.domain.usecase.EndShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.FindPersonaUseCase
import com.dur1al.controlate.porteria.domain.usecase.GetActiveShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.MarkExitUseCase
import com.dur1al.controlate.porteria.domain.usecase.RegisterBikeEntryUseCase
import com.dur1al.controlate.porteria.domain.usecase.RegisterEntryOutcome
import com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase
import com.dur1al.controlate.porteria.domain.validation.Rut
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

/** Bitácora del bicicletero: ingresos por RUT, salidas con deshacer y cierre de turno. */
@OptIn(ExperimentalCoroutinesApi::class)
class BicicleteroViewModel(
    private val getActiveShift: GetActiveShiftUseCase,
    private val registerEntry: RegisterBikeEntryUseCase,
    private val markExit: MarkExitUseCase,
    private val findPersona: FindPersonaUseCase,
    entries: BikeEntryRepository,
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
        val registros: List<RegistroBicicleta> = emptyList(),
        val busy: Boolean = false
    )

    sealed interface Event {
        data object NoActiveShift : Event
        data object ShiftEnded : Event
        data class ExitMarked(val id: Long, val at: Long, val nombre: String) : Event
        data class Share(val file: java.io.File) : Event
        data class Message(val message: UiMessage) : Event
    }

    sealed interface FormEvent {
        data object Saved : FormEvent
        data class Autofill(val data: FindPersonaUseCase.BikeAutofill) : FormEvent
        data class Failed(val message: UiMessage) : FormEvent
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _formErrors = MutableStateFlow<List<ValidationError>>(emptyList())
    val formErrors: StateFlow<List<ValidationError>> = _formErrors.asStateFlow()

    private val eventChannel = EventChannel<Event>()
    val events: Flow<Event> = eventChannel.events

    private val formChannel = EventChannel<FormEvent>()
    val formEvents: Flow<FormEvent> = formChannel.events

    /** `true` mientras se guarda un ingreso: el formulario deshabilita "Registrar" (evita duplicados por doble toque). */
    private val _formSaving = MutableStateFlow(false)
    val formSaving: StateFlow<Boolean> = _formSaving.asStateFlow()

    /** Último autocompletado ofrecido en el formulario abierto: evita repetir el aviso. */
    private var lastAutofill: FindPersonaUseCase.BikeAutofill? = null

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

    /** La búsqueda acepta RUT con o sin formato, o parte del nombre. */
    fun setQuery(value: String) {
        val trimmed = value.trim()
        val looksLikeRut = trimmed.isNotEmpty() && trimmed.all { it.isDigit() || it in ".-kK " }
        query.value = if (looksLikeRut) Rut.normalize(trimmed) else trimmed
        _state.update { it.copy(query = value) }
    }

    /** Se llama al abrir el formulario: parte sin errores ni autocompletado previo. */
    fun onFormOpened() {
        _formErrors.value = emptyList()
        lastAutofill = null
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
            ValidationError.Field.RUT -> validator.rut(value, required = true)
            ValidationError.Field.NOMBRE -> validator.nombre(value)
            ValidationError.Field.CARRERA -> validator.carrera(value)
            else -> null
        } ?: return
        _formErrors.update { errors -> errors.filterNot { it.field == field } + error }
    }

    fun register(input: BikeEntryInput) {
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

    fun lookupRut(raw: String) {
        viewModelScope.launch {
            val data = findPersona.bikeAutofill(raw) ?: return@launch
            if (data == lastAutofill) return@launch
            lastAutofill = data
            formChannel.send(FormEvent.Autofill(data))
        }
    }

    fun markExit(registro: RegistroBicicleta) {
        viewModelScope.launch {
            val at = markExit.bike(registro.id)
            eventChannel.send(
                if (at != null) Event.ExitMarked(registro.id, at, registro.nombre)
                else Event.Message(UiMessage(R.string.exit_already_registered))
            )
        }
    }

    fun undoExit(id: Long, at: Long) {
        viewModelScope.launch {
            if (markExit.undoBike(id, at)) eventChannel.send(Event.Message(UiMessage(R.string.exit_undone)))
        }
    }

    /** Genera el PDF del turno en curso y lo ofrece para compartir. */
    fun exportCurrentShift() {
        val id = shiftId.value ?: return
        viewModelScope.launch {
            val doc = buildReport.shift(id)
            if (doc == null || doc.rows.isEmpty()) {
                eventChannel.send(Event.Message(UiMessage(R.string.bici_export_empty)))
                return@launch
            }
            runCatchingCancellable { exporter.export(doc) }
                .onSuccess { eventChannel.send(Event.Share(it)) }
                .onFailure { eventChannel.send(Event.Message(UiMessage(R.string.error_generic))) }
        }
    }

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
