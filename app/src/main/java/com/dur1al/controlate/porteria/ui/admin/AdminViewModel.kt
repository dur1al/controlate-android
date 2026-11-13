package com.dur1al.controlate.porteria.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.PointStat
import com.dur1al.controlate.porteria.domain.report.ReportExporter
import com.dur1al.controlate.porteria.domain.usecase.BuildReportUseCase
import com.dur1al.controlate.porteria.domain.usecase.ClearOperationalDataUseCase
import com.dur1al.controlate.porteria.domain.usecase.GetDailyStatsUseCase
import com.dur1al.controlate.porteria.ui.common.EventChannel
import com.dur1al.controlate.porteria.ui.common.UiMessage
import com.dur1al.controlate.porteria.ui.common.runCatchingCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** Panel administrador: estadísticas del día, resumen PDF y limpieza de datos operativos. */
class AdminViewModel(
    private val getDailyStats: GetDailyStatsUseCase,
    private val clearOperationalData: ClearOperationalDataUseCase,
    private val buildReport: BuildReportUseCase,
    private val exporter: ReportExporter
) : ViewModel() {

    data class Totals(val entradas: Int, val salidas: Int, val dentro: Int)

    data class UiState(
        val loading: Boolean = true,
        val points: List<PointStat> = emptyList(),
        /** `null` = vista general (todos los puntos). */
        val scope: ControlPoint? = null,
        val totals: Totals = Totals(0, 0, 0),
        val busy: Boolean = false
    )

    sealed interface Event {
        data class Share(val file: File) : Event
        data object Cleared : Event
        data class Message(val message: UiMessage) : Event
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val eventChannel = EventChannel<Event>()
    val events: Flow<Event> = eventChannel.events

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val points = getDailyStats()
            _state.update { it.copy(loading = false, points = points, totals = totalsFor(points, it.scope)) }
        }
    }

    fun selectScope(point: ControlPoint?) {
        _state.update { it.copy(scope = point, totals = totalsFor(it.points, point)) }
    }

    fun exportSummary() {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            runCatchingCancellable { exporter.export(buildReport.dailySummary(getDailyStats())) }
                .onSuccess { eventChannel.send(Event.Share(it)) }
                .onFailure { eventChannel.send(Event.Message(UiMessage(R.string.error_generic))) }
            _state.update { it.copy(busy = false) }
        }
    }

    fun clearOperationalData() {
        viewModelScope.launch {
            clearOperationalData.invoke()
            eventChannel.send(Event.Cleared)
            refresh()
        }
    }

    private fun totalsFor(points: List<PointStat>, scope: ControlPoint?): Totals {
        val selected = if (scope == null) points else points.filter { it.controlPoint == scope }
        return Totals(selected.sumOf { it.entradas }, selected.sumOf { it.salidas }, selected.sumOf { it.dentro })
    }
}
