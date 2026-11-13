package com.dur1al.controlate.porteria.ui.db

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.domain.importer.PersonaCsvParser
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.report.ReportExporter
import com.dur1al.controlate.porteria.domain.repository.PersonaRepository
import com.dur1al.controlate.porteria.domain.usecase.BuildReportUseCase
import com.dur1al.controlate.porteria.domain.usecase.ImportPersonasUseCase
import com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase
import com.dur1al.controlate.porteria.domain.validation.Patente
import com.dur1al.controlate.porteria.domain.validation.Rut
import com.dur1al.controlate.porteria.ui.common.EventChannel
import com.dur1al.controlate.porteria.ui.common.UiMessage
import com.dur1al.controlate.porteria.ui.common.runCatchingCancellable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Gestión del directorio (solo administrador). El CRUD es trivial, por lo que se usa el
 * repositorio directamente; la importación y el reporte pasan por casos de uso.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DirectoryViewModel(
    private val personas: PersonaRepository,
    private val importPersonas: ImportPersonasUseCase,
    private val buildReport: BuildReportUseCase,
    private val exporter: ReportExporter,
    private val validator: ValidateRegistroUseCase
) : ViewModel() {

    sealed interface Event {
        data class Share(val file: File) : Event
        data class ImportFailed(val errors: List<PersonaCsvParser.LineError>) : Event
        data class Message(val message: UiMessage) : Event

        /** La edición se guardó: el diálogo puede cerrarse. */
        data object EditSaved : Event
    }

    /** Errores del diálogo de edición; mientras haya alguno el diálogo sigue abierto. */
    private val _editErrors = MutableStateFlow<List<ValidationError>>(emptyList())
    val editErrors: StateFlow<List<ValidationError>> = _editErrors.asStateFlow()

    private val query = MutableStateFlow("")

    val results: StateFlow<List<Persona>?> = query
        .flatMapLatest { personas.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val eventChannel = EventChannel<Event>()
    val events: Flow<Event> = eventChannel.events

    fun setQuery(value: String) {
        query.value = value.trim()
    }

    fun startEdit() {
        _editErrors.value = emptyList()
    }

    fun clearEditError(field: ValidationError.Field) {
        _editErrors.update { errors -> errors.filterNot { it.field == field } }
    }

    /**
     * Valida con las mismas reglas que el ingreso (patente, nombre y RUT) y traduce la colisión
     * con el índice único de patente a un error del campo, en vez de cerrar la app.
     */
    fun update(persona: Persona) {
        val errors = validator.validatePersona(persona.patente, persona.nombreApellido, persona.rut)
        if (errors.isNotEmpty()) {
            _editErrors.value = errors
            return
        }
        val patente = Patente.normalize(persona.patente)
        viewModelScope.launch {
            val existing = personas.getByPatente(patente)
            if (existing != null && existing.id != persona.id) {
                _editErrors.value = listOf(ValidationError(ValidationError.Field.PATENTE, ValidationError.Reason.DUPLICATE))
                return@launch
            }
            val normalized = persona.copy(
                patente = patente,
                nombreApellido = persona.nombreApellido.trim(),
                rut = Rut.normalizeOrNull(persona.rut)
            )
            runCatchingCancellable { personas.update(normalized) }
                .onSuccess {
                    _editErrors.value = emptyList()
                    eventChannel.send(Event.EditSaved)
                    eventChannel.send(Event.Message(UiMessage(R.string.directory_updated)))
                }
                .onFailure { eventChannel.send(Event.Message(UiMessage(R.string.error_generic))) }
        }
    }

    fun delete(persona: Persona) {
        viewModelScope.launch {
            personas.delete(persona)
            eventChannel.send(Event.Message(UiMessage(R.string.directory_deleted)))
        }
    }

    fun import(content: String) {
        if (content.isBlank()) return eventChannel.send(Event.Message(UiMessage(R.string.settings_import_paste_error)))
        viewModelScope.launch {
            val result = importPersonas(content)
            eventChannel.send(
                if (result.errors.isNotEmpty()) Event.ImportFailed(result.errors)
                else Event.Message(UiMessage(R.string.db_import_success, listOf(result.personas.size)))
            )
        }
    }

    fun exportDirectory() {
        viewModelScope.launch {
            val doc = buildReport.directory()
            if (doc.rows.isEmpty()) {
                eventChannel.send(Event.Message(UiMessage(R.string.db_no_directory_data)))
                return@launch
            }
            runCatchingCancellable { exporter.export(doc) }
                .onSuccess { eventChannel.send(Event.Share(it)) }
                .onFailure { eventChannel.send(Event.Message(UiMessage(R.string.error_generic))) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
