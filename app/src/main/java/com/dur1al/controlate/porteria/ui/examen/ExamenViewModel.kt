package com.dur1al.controlate.porteria.ui.examen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.data.local.entities.ExamenResultado
import com.dur1al.controlate.porteria.domain.usecase.CalcularResultadoExamenUseCase
import com.dur1al.controlate.porteria.domain.usecase.GenerarExamenUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Simulador OS-10. El temporizador es una corrutina en `viewModelScope` (sobrevive a rotaciones,
 * se cancela con el ViewModel y se prueba con tiempo virtual), en vez de `CountDownTimer`.
 */
class ExamenViewModel(
    private val generar: GenerarExamenUseCase,
    private val calcular: CalcularResultadoExamenUseCase,
    private val guardiaNombre: String,
    private val durationSeconds: Long = GenerarExamenUseCase.DURACION_MINUTOS * 60L
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val preguntas: List<ExamenPregunta> = emptyList(),
        val index: Int = 0,
        val respuestas: Map<Int, Int> = emptyMap(),
        val remainingSeconds: Long = 0,
        val resultado: ExamenResultado? = null
    ) {
        val current: ExamenPregunta? get() = preguntas.getOrNull(index)
        val selectedOption: Int? get() = respuestas[index]
        val isFirst: Boolean get() = index == 0
        val isLast: Boolean get() = index == preguntas.lastIndex
        val answeredCount: Int get() = respuestas.size
    }

    private val _state = MutableStateFlow(UiState(remainingSeconds = durationSeconds))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var timer: Job? = null
    private var elapsedSeconds = 0L
    private var finishing = false

    init {
        viewModelScope.launch {
            val preguntas = generar()
            _state.update { it.copy(loading = false, preguntas = preguntas) }
            if (preguntas.isNotEmpty()) startTimer()
        }
    }

    private fun startTimer() {
        timer = viewModelScope.launch {
            while (_state.value.remainingSeconds > 0) {
                delay(1_000)
                elapsedSeconds++
                _state.update { it.copy(remainingSeconds = (durationSeconds - elapsedSeconds).coerceAtLeast(0)) }
            }
            finish()
        }
    }

    fun answer(option: Int) {
        if (option !in 0..3 || _state.value.resultado != null) return
        _state.update { it.copy(respuestas = it.respuestas + (it.index to option)) }
    }

    fun next() = goTo(_state.value.index + 1)

    fun previous() = goTo(_state.value.index - 1)

    fun goTo(index: Int) {
        _state.update { if (index in it.preguntas.indices) it.copy(index = index) else it }
    }

    /** Corrige y guarda el intento. Idempotente: llamadas repetidas (botón + temporizador) no duplican. */
    fun finish() {
        if (finishing) return
        finishing = true
        timer?.cancel()
        val snapshot = _state.value
        viewModelScope.launch {
            val resultado = calcular(guardiaNombre, snapshot.preguntas, snapshot.respuestas, elapsedSeconds)
            _state.update { it.copy(resultado = resultado) }
        }
    }
}
