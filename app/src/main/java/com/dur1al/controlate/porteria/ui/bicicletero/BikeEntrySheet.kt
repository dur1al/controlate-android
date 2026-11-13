package com.dur1al.controlate.porteria.ui.bicicletero

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.BottomSheetBicicletaBinding
import com.dur1al.controlate.porteria.domain.model.BikeEntryInput
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.usecase.FindPersonaUseCase
import com.dur1al.controlate.porteria.domain.validation.Rut
import com.dur1al.controlate.porteria.ui.common.applyRutFormatter
import com.dur1al.controlate.porteria.ui.common.collectWhenStarted
import com.dur1al.controlate.porteria.ui.common.messageRes
import com.dur1al.controlate.porteria.ui.common.textTrimmed
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/** Formulario de ingreso al bicicletero; autocompleta por RUT desde el historial o el directorio. */
class BikeEntrySheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBicicletaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BicicleteroViewModel by activityViewModel()
    private var lookupJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Controlate_BottomSheet)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetBicicletaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.clearFormErrors()
        if (savedInstanceState == null) binding.rbEstudiante.isChecked = true

        binding.etRutBici.applyRutFormatter()
        binding.etRutBici.doAfterTextChanged { text ->
            binding.tilRutBici.error = null
            val raw = text?.toString().orEmpty()
            if (Rut.isValid(raw)) {
                lookupJob?.cancel()
                lookupJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(LOOKUP_DEBOUNCE_MS)
                    viewModel.lookupRut(raw)
                }
            }
        }
        binding.etNombreBici.doAfterTextChanged { binding.tilNombreBici.error = null }
        binding.etCarreraBici.doAfterTextChanged { binding.tilCarreraBici.error = null }
        binding.btnGuardarBici.setOnClickListener { viewModel.register(readInput()) }

        collectWhenStarted(viewModel.formErrors, ::showErrors)
        collectWhenStarted(viewModel.formEvents) { event ->
            when (event) {
                BicicleteroViewModel.FormEvent.Saved -> {
                    Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.entry_registered, Snackbar.LENGTH_SHORT).show()
                    dismiss()
                }
                is BicicleteroViewModel.FormEvent.Autofill -> autofill(event.data)
            }
        }
    }

    private fun readInput() = BikeEntryInput(
        rut = binding.etRutBici.textTrimmed(),
        nombre = binding.etNombreBici.textTrimmed(),
        carrera = binding.etCarreraBici.textTrimmed(),
        tipoPersona = when (binding.rgTipoPersonaBici.checkedRadioButtonId) {
            binding.rbFuncionario.id -> PersonaType.FUNCIONARIO
            binding.rbVisita.id -> PersonaType.VISITA
            else -> PersonaType.ESTUDIANTE
        },
        observacion = binding.etObsBici.textTrimmed()
    )

    private fun showErrors(errors: List<ValidationError>) {
        val byField = errors.associateBy { it.field }
        binding.tilRutBici.error = byField[ValidationError.Field.RUT]?.let { getString(it.messageRes) }
        binding.tilNombreBici.error = byField[ValidationError.Field.NOMBRE]?.let { getString(it.messageRes) }
        binding.tilCarreraBici.error = byField[ValidationError.Field.CARRERA]?.let { getString(it.messageRes) }
    }

    private fun autofill(data: FindPersonaUseCase.BikeAutofill) = with(binding) {
        if (etNombreBici.text.isNullOrBlank()) etNombreBici.setText(data.nombre)
        if (etCarreraBici.text.isNullOrBlank()) etCarreraBici.setText(data.carrera)
        when (data.tipoPersona) {
            PersonaType.FUNCIONARIO -> rbFuncionario.isChecked = true
            PersonaType.VISITA -> rbVisita.isChecked = true
            else -> rbEstudiante.isChecked = true
        }
        Snackbar.make(root, R.string.autofill_match_found, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        lookupJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "BikeEntrySheet"
        private const val LOOKUP_DEBOUNCE_MS = 250L
    }
}
