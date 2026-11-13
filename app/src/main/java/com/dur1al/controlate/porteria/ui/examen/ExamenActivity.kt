package com.dur1al.controlate.porteria.ui.examen

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityExamenBinding
import com.dur1al.controlate.porteria.data.local.session.SessionManager
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class ExamenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExamenBinding
    private val viewModel: ExamenViewModel by viewModel()
    private lateinit var sessionManager: SessionManager

    private var isUpdatingRadioGroup = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExamenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setupToolbar()
        setupListeners()
        setupObservers()
        setupOnBackPressed()

        val guardiaNombre = sessionManager.getGuardiaName().ifBlank { "Guardia OS-10" }
        viewModel.iniciarExamen(guardiaNombre)
    }

    private fun setupToolbar() {
        binding.toolbarExamen.setNavigationOnClickListener {
            confirmarSalirExamen()
        }
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmarSalirExamen()
            }
        })
    }

    private fun setupListeners() {
        binding.rgOpciones.setOnCheckedChangeListener { _, checkedId ->
            if (isUpdatingRadioGroup) return@setOnCheckedChangeListener
            val seleccionIndex = when (checkedId) {
                R.id.rbOpcionA -> 0
                R.id.rbOpcionB -> 1
                R.id.rbOpcionC -> 2
                R.id.rbOpcionD -> 3
                else -> -1
            }
            if (seleccionIndex != -1) {
                viewModel.seleccionarRespuesta(seleccionIndex)
            }
        }

        binding.btnAnterior.setOnClickListener {
            viewModel.anteriorPregunta()
        }

        binding.btnSiguiente.setOnClickListener {
            viewModel.siguientePregunta()
        }

        binding.btnFinalizar.setOnClickListener {
            confirmarFinalizarExamen()
        }
    }

    private fun setupObservers() {
        viewModel.preguntas.observe(this) { preguntas ->
            if (preguntas.isNotEmpty()) {
                val total = preguntas.size
                binding.progressExamen.max = total
                renderPreguntaActual()
            }
        }

        viewModel.indiceActual.observe(this) { _ ->
            renderPreguntaActual()
        }

        viewModel.respuestas.observe(this) { _ ->
            updateSeleccionRadioGroup()
        }

        viewModel.tiempoFormateado.observe(this) { tiempoStr ->
            binding.tvTimer.text = tiempoStr
        }

        viewModel.resultado.observe(this) { resultado ->
            if (resultado != null) {
                val dialog = ExamenResultadoDialogFragment.newInstance(resultado)
                dialog.setOnDismissListener {
                    finish()
                }
                dialog.show(supportFragmentManager, ExamenResultadoDialogFragment.TAG)
            }
        }
    }

    private fun renderPreguntaActual() {
        val lista = viewModel.preguntas.value ?: return
        if (lista.isEmpty()) return

        val index = viewModel.indiceActual.value ?: 0
        val total = lista.size
        val pregunta = lista.getOrNull(index) ?: return

        binding.progressExamen.progress = index + 1
        binding.tvPreguntaContador.text = String.format(Locale.getDefault(), "Pregunta %d de %d", index + 1, total)
        binding.tvCategoria.text = pregunta.categoria
        binding.tvPreguntaTexto.text = pregunta.pregunta

        binding.rbOpcionA.text = String.format(Locale.getDefault(), "A) %s", pregunta.opcionA)
        binding.rbOpcionB.text = String.format(Locale.getDefault(), "B) %s", pregunta.opcionB)
        binding.rbOpcionC.text = String.format(Locale.getDefault(), "C) %s", pregunta.opcionC)
        binding.rbOpcionD.text = String.format(Locale.getDefault(), "D) %s", pregunta.opcionD)

        binding.btnAnterior.isEnabled = index > 0
        binding.btnSiguiente.isVisible = index < total - 1
        binding.btnFinalizar.isVisible = index == total - 1 || viewModel.respuestas.value?.size == total

        updateSeleccionRadioGroup()
    }

    private fun updateSeleccionRadioGroup() {
        val index = viewModel.indiceActual.value ?: 0
        val seleccion = viewModel.respuestas.value?.get(index)

        isUpdatingRadioGroup = true
        binding.rgOpciones.clearCheck()

        when (seleccion) {
            0 -> binding.rbOpcionA.isChecked = true
            1 -> binding.rbOpcionB.isChecked = true
            2 -> binding.rbOpcionC.isChecked = true
            3 -> binding.rbOpcionD.isChecked = true
        }
        isUpdatingRadioGroup = false
    }

    private fun confirmarSalirExamen() {
        AlertDialog.Builder(this)
            .setTitle("Salir del Examen")
            .setMessage("¿Deseas salir del examen? Se perderá el progreso del intento actual.")
            .setPositiveButton("Sí, salir") { _, _ -> finish() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarFinalizarExamen() {
        val respondidas = viewModel.respuestas.value?.size ?: 0
        val total = viewModel.preguntas.value?.size ?: 60

        val mensaje = if (respondidas < total) {
            String.format(Locale.getDefault(), "Has respondido %d de %d preguntas. ¿Deseas entregar el examen ahora?", respondidas, total)
        } else {
            "Has respondido todas las preguntas (60/60). ¿Deseas entregar el examen?"
        }

        AlertDialog.Builder(this)
            .setTitle("Finalizar Examen OS-10")
            .setMessage(mensaje)
            .setPositiveButton("Entregar Examen") { _, _ ->
                viewModel.finalizarExamen()
            }
            .setNegativeButton("Continuar respondiendo", null)
            .show()
    }
}
