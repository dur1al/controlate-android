package com.dur1al.controlate.porteria.ui.examen

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.data.local.entities.ExamenResultado
import com.dur1al.controlate.porteria.domain.usecase.CalcularResultadoExamenUseCase
import com.dur1al.controlate.porteria.domain.usecase.GenerarExamenUseCase
import kotlinx.coroutines.launch
import java.util.Locale

class ExamenViewModel(
    private val generarExamenUseCase: GenerarExamenUseCase,
    private val calcularResultadoExamenUseCase: CalcularResultadoExamenUseCase
) : ViewModel() {

    private val _preguntas = MutableLiveData<List<ExamenPregunta>>(emptyList())
    val preguntas: LiveData<List<ExamenPregunta>> = _preguntas

    private val _indiceActual = MutableLiveData<Int>(0)
    val indiceActual: LiveData<Int> = _indiceActual

    private val _respuestas = MutableLiveData<MutableMap<Int, Int>>(mutableMapOf())
    val respuestas: LiveData<MutableMap<Int, Int>> = _respuestas

    private val _tiempoFormateado = MutableLiveData<String>("60:00")
    val tiempoFormateado: LiveData<String> = _tiempoFormateado

    private val _resultado = MutableLiveData<ExamenResultado?>()
    val resultado: LiveData<ExamenResultado?> = _resultado

    private val _cargando = MutableLiveData<Boolean>(true)
    val cargando: LiveData<Boolean> = _cargando

    private var guardiaNombre: String = "Guardia OS-10"
    private var timer: CountDownTimer? = null
    private var segundosTotales: Long = TIEMPO_EXAMEN_MINUTOS * 60L
    private var segundosTranscurridos: Long = 0L

    fun iniciarExamen(nombreGuardia: String = "Guardia OS-10") {
        this.guardiaNombre = nombreGuardia
        _cargando.value = true
        viewModelScope.launch {
            val lista = generarExamenUseCase(GenerarExamenUseCase.CANTIDAD_OFICIAL_PREGUNTAS)
            _preguntas.postValue(lista)
            _indiceActual.postValue(0)
            _respuestas.postValue(mutableMapOf())
            _cargando.postValue(false)
            iniciarTemporizador()
        }
    }

    private fun iniciarTemporizador() {
        timer?.cancel()
        val tiempoMillis = segundosTotales * 1000L
        timer = object : CountDownTimer(tiempoMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                segundosTranscurridos++
                val segundosRestantes = millisUntilFinished / 1000L
                val minutos = segundosRestantes / 60
                val segs = segundosRestantes % 60
                _tiempoFormateado.postValue(String.format(Locale.getDefault(), "%02d:%02d", minutos, segs))
            }

            override fun onFinish() {
                _tiempoFormateado.postValue("00:00")
                finalizarExamen()
            }
        }.start()
    }

    fun seleccionarRespuesta(opcionIndex: Int) {
        val mapaActual = _respuestas.value ?: mutableMapOf()
        val indice = _indiceActual.value ?: 0
        mapaActual[indice] = opcionIndex
        _respuestas.value = mapaActual
    }

    fun siguientePregunta() {
        val actual = _indiceActual.value ?: 0
        val total = _preguntas.value?.size ?: 0
        if (actual < total - 1) {
            _indiceActual.value = actual + 1
        }
    }

    fun anteriorPregunta() {
        val actual = _indiceActual.value ?: 0
        if (actual > 0) {
            _indiceActual.value = actual - 1
        }
    }

    fun irAPregunta(posicion: Int) {
        val total = _preguntas.value?.size ?: 0
        if (posicion in 0 until total) {
            _indiceActual.value = posicion
        }
    }

    fun finalizarExamen() {
        timer?.cancel()
        val listaPreguntas = _preguntas.value ?: return
        val mapaRespuestas = _respuestas.value ?: emptyMap()

        viewModelScope.launch {
            val res = calcularResultadoExamenUseCase(
                guardiaNombre = guardiaNombre,
                preguntas = listaPreguntas,
                respuestasUsuario = mapaRespuestas,
                tiempoEmpleadoSegundos = segundosTranscurridos
            )
            _resultado.postValue(res)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }

    companion object {
        const val TIEMPO_EXAMEN_MINUTOS = 60
    }
}
