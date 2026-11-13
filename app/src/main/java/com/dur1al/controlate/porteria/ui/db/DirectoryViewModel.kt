package com.dur1al.controlate.porteria.ui.db

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.domain.importer.PersonaCsvParser
import com.dur1al.controlate.porteria.domain.report.ReportExporter
import com.dur1al.controlate.porteria.domain.repository.PersonaRepository
import com.dur1al.controlate.porteria.domain.usecase.BuildReportUseCase
import com.dur1al.controlate.porteria.domain.usecase.ImportPersonasUseCase
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
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
    private val exporter: ReportExporter
) : ViewModel() {

    sealed interface Event {
        data class Share(val file: File) : Event
        data class ImportFailed(val errors: List<PersonaCsvParser.LineError>) : Event
        data class Message(val message: UiMessage) : Event
    }

    private val query = MutableStateFlow("")

    val results: StateFlow<List<Persona>?> = query
        .flatMapLatest { personas.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val eventChannel = EventChannel<Event>()
    val events: Flow<Event> = eventChannel.events

    fun setQuery(value: String) {
        query.value = value.trim()
    }

    fun update(persona: Persona) {
        viewModelScope.launch {
            personas.update(
                persona.copy(
                    patente = Patente.normalize(persona.patente),
                    rut = Rut.normalizeOrNull(persona.rut)
                )
            )
            eventChannel.send(Event.Message(UiMessage(R.string.directory_updated)))
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
