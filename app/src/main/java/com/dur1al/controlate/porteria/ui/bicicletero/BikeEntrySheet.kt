package com.dur1al.controlate.porteria.ui.bicicletero

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.dur1al.controlate.porteria.ui.common.expandFully
import com.dur1al.controlate.porteria.ui.common.focusFirstError
import com.dur1al.controlate.porteria.ui.common.messageRes
import com.dur1al.controlate.porteria.ui.common.showFormStatus
import com.dur1al.controlate.porteria.ui.common.textTrimmed
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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

    /** Tras pulsar "Registrar", el siguiente render de errores lleva el foco al primero. */
    private var focusErrorOnRender = false

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
        if (savedInstanceState == null) {
            viewModel.onFormOpened()
            binding.rbEstudiante.isChecked = true
        }

        binding.etRutBici.applyRutFormatter()
        binding.etRutBici.doAfterTextChanged { text ->
            viewModel.clearFieldError(ValidationError.Field.RUT)
            val raw = text?.toString().orEmpty()
            if (Rut.isValid(raw)) {
                lookupJob?.cancel()
                lookupJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(LOOKUP_DEBOUNCE_MS)
                    viewModel.lookupRut(raw)
                }
            }
        }
        binding.etNombreBici.doAfterTextChanged { viewModel.clearFieldError(ValidationError.Field.NOMBRE) }
        binding.etCarreraBici.doAfterTextChanged { viewModel.clearFieldError(ValidationError.Field.CARRERA) }
        validateOnBlur(binding.etRutBici, ValidationError.Field.RUT)
        validateOnBlur(binding.etNombreBici, ValidationError.Field.NOMBRE)
        validateOnBlur(binding.etCarreraBici, ValidationError.Field.CARRERA)
        binding.btnGuardarBici.setOnClickListener {
            focusErrorOnRender = true
            viewModel.register(readInput())
        }

        collectWhenStarted(viewModel.formErrors, ::showErrors)
        collectWhenStarted(viewModel.formSaving) { saving -> binding.btnGuardarBici.isEnabled = !saving }
        collectWhenStarted(viewModel.formEvents) { event ->
            when (event) {
                // La confirmación la muestra la pantalla principal, anclada sobre su botón.
                BicicleteroViewModel.FormEvent.Saved -> dismiss()
                is BicicleteroViewModel.FormEvent.Autofill -> autofill(event.data)
                is BicicleteroViewModel.FormEvent.Failed -> {
                    focusErrorOnRender = false
                    binding.tvFormStatus.showFormStatus(event.message.resolve(requireContext()), R.color.error_red)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        expandFully()
    }

    private fun validateOnBlur(field: EditText, which: ValidationError.Field) {
        field.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && _binding != null) viewModel.onFieldBlur(which, field.textTrimmed())
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
        if (focusErrorOnRender && errors.isNotEmpty()) {
            focusErrorOnRender = false
            focusFirstError(listOf(binding.tilRutBici, binding.tilNombreBici, binding.tilCarreraBici))
        }
    }

    private fun autofill(data: FindPersonaUseCase.BikeAutofill) = with(binding) {
        if (etNombreBici.text.isNullOrBlank()) etNombreBici.setText(data.nombre)
        if (etCarreraBici.text.isNullOrBlank()) etCarreraBici.setText(data.carrera)
        when (data.tipoPersona) {
            PersonaType.FUNCIONARIO -> rbFuncionario.isChecked = true
            PersonaType.VISITA -> rbVisita.isChecked = true
            else -> rbEstudiante.isChecked = true
        }
        // Aviso en línea: un snackbar dentro del sheet tapaba el botón "Registrar".
        tvFormStatus.showFormStatus(getString(R.string.autofill_match_found), R.color.bicicletero2_primary)
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
