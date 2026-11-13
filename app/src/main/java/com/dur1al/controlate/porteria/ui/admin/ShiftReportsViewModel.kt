package com.dur1al.controlate.porteria.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.report.ReportStore
import com.dur1al.controlate.porteria.domain.model.ShiftSummary
import com.dur1al.controlate.porteria.domain.report.ReportExporter
import com.dur1al.controlate.porteria.domain.repository.ShiftRepository
import com.dur1al.controlate.porteria.domain.usecase.BuildReportUseCase
import com.dur1al.controlate.porteria.ui.common.EventChannel
import com.dur1al.controlate.porteria.ui.common.UiMessage
import com.dur1al.controlate.porteria.ui.common.runCatchingCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** Listado de turnos (porterías y bicicleteros) con filtro por fecha, exportación y borrado. */
class ShiftReportsViewModel(
    private val shifts: ShiftRepository,
    private val buildReport: BuildReportUseCase,
    private val exporter: ReportExporter,
    private val store: ReportStore
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        /** Fecha ISO (`yyyy-MM-dd`) del filtro activo, o `null`. */
        val dateFilter: String? = null,
        val shifts: List<ShiftSummary> = emptyList()
    )

    sealed interface Event {
        data class Share(val file: File) : Event
        data class SavedReports(val files: List<File>) : Event
        data class Message(val message: UiMessage) : Event
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val eventChannel = EventChannel<Event>()
    val events: Flow<Event> = eventChannel.events

    private val dateFilter = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            combine(shifts.observeSummaries(), dateFilter) { all, date ->
                date to (if (date == null) all else all.filter { it.fecha == date })
            }.collect { (date, list) ->
                _state.update { it.copy(loading = false, dateFilter = date, shifts = list) }
            }
        }
    }

    fun filterByDate(isoDate: String?) {
        dateFilter.value = isoDate
    }

    fun export(shiftId: Long) {
        viewModelScope.launch {
            val doc = buildReport.shift(shiftId)
            if (doc == null || doc.rows.isEmpty()) {
                eventChannel.send(Event.Message(UiMessage(R.string.reports_export_empty)))
                return@launch
            }
            runCatchingCancellable { exporter.export(doc) }
                .onSuccess { eventChannel.send(Event.Share(it)) }
                .onFailure { eventChannel.send(Event.Message(UiMessage(R.string.error_generic))) }
        }
    }

    fun delete(shiftId: Long) {
        viewModelScope.launch { shifts.delete(shiftId) }
    }

    fun loadSavedReports() {
        viewModelScope.launch {
            val files = store.list()
            eventChannel.send(
                if (files.isEmpty()) Event.Message(UiMessage(R.string.reports_saved_empty))
                else Event.SavedReports(files)
            )
        }
    }
}
