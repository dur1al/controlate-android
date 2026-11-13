package com.dur1al.controlate.porteria.ui.examen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityExamenBinding
import com.dur1al.controlate.porteria.ui.common.collectWhenStarted
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/** Simulador del examen OS-10 (60 preguntas, 60 minutos, aprobación con 70 %). */
class ExamenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExamenBinding
    private val viewModel: ExamenViewModel by viewModel {
        parametersOf(intent.getStringExtra(EXTRA_GUARD_NAME).orEmpty())
    }

    /** Evita que actualizar los RadioButton desde el estado dispare una nueva respuesta. */
    private var rendering = false
    private var resultShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExamenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        resultShown = supportFragmentManager.findFragmentByTag(ExamenResultadoDialogFragment.TAG) != null

        binding.toolbarExamen.setNavigationOnClickListener { confirmExit() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = confirmExit()
        })
        binding.rgOpciones.setOnCheckedChangeListener { _, checkedId ->
            if (rendering) return@setOnCheckedChangeListener
            optionIds.indexOf(checkedId).takeIf { it >= 0 }?.let(viewModel::answer)
        }
        binding.btnAnterior.setOnClickListener { viewModel.previous() }
        binding.btnSiguiente.setOnClickListener { viewModel.next() }
        binding.btnFinalizar.setOnClickListener { confirmFinish() }

        collectWhenStarted(viewModel.state, ::render)
    }

    private val optionIds by lazy {
        listOf(binding.rbOpcionA.id, binding.rbOpcionB.id, binding.rbOpcionC.id, binding.rbOpcionD.id)
    }

    private fun render(state: ExamenViewModel.UiState) = with(binding) {
        val minutes = state.remainingSeconds / 60
        val seconds = state.remainingSeconds % 60
        tvTimer.text = getString(R.string.examen_timer_format, minutes, seconds)

        val pregunta = state.current
        if (!state.loading && pregunta == null) {
            Snackbar.make(root, R.string.examen_empty_bank, Snackbar.LENGTH_LONG).show()
        }
        if (pregunta != null) {
            progressExamen.max = state.preguntas.size
            progressExamen.progress = state.index + 1
            tvPreguntaContador.text = getString(R.string.examen_pregunta_format, state.index + 1, state.preguntas.size)
            tvCategoria.text = pregunta.categoria
            tvPreguntaTexto.text = pregunta.pregunta
            rbOpcionA.text = getString(R.string.examen_option_format, "A", pregunta.opcionA)
            rbOpcionB.text = getString(R.string.examen_option_format, "B", pregunta.opcionB)
            rbOpcionC.text = getString(R.string.examen_option_format, "C", pregunta.opcionC)
            rbOpcionD.text = getString(R.string.examen_option_format, "D", pregunta.opcionD)
            rendering = true
            val selected = state.selectedOption
            if (selected == null) rgOpciones.clearCheck() else rgOpciones.check(optionIds[selected])
            rendering = false
        }
        btnAnterior.isEnabled = !state.isFirst
        btnSiguiente.isVisible = !state.isLast
        btnFinalizar.isVisible = state.isLast || state.answeredCount == state.preguntas.size

        val resultado = state.resultado
        if (resultado != null && !resultShown) {
            resultShown = true
            ExamenResultadoDialogFragment.newInstance(resultado)
                .show(supportFragmentManager, ExamenResultadoDialogFragment.TAG)
        }
    }

    private fun confirmExit() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.examen_exit_title)
            .setMessage(R.string.examen_exit_message)
            .setPositiveButton(R.string.examen_exit_confirm) { _, _ -> finish() }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun confirmFinish() {
        val state = viewModel.state.value
        val message = if (state.answeredCount < state.preguntas.size) {
            getString(R.string.examen_finish_partial, state.answeredCount, state.preguntas.size)
        } else {
            getString(R.string.examen_finish_complete)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.examen_finish_title)
            .setMessage(message)
            .setPositiveButton(R.string.examen_finish_confirm) { _, _ -> viewModel.finish() }
            .setNegativeButton(R.string.examen_keep_answering, null)
            .show()
    }

    companion object {
        private const val EXTRA_GUARD_NAME = "guard_name"

        fun intent(context: Context, guardName: String) =
            Intent(context, ExamenActivity::class.java).putExtra(EXTRA_GUARD_NAME, guardName)
    }
}
