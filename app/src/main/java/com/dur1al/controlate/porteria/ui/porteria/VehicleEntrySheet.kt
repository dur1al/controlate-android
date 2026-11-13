package com.dur1al.controlate.porteria.ui.porteria

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.BottomSheetEntradaBinding
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.model.VehicleEntryInput
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

/**
 * Formulario de ingreso vehicular. En modo detalle muestra un registro existente en solo lectura
 * (editar registros es exclusivo del administrador).
 */
class VehicleEntrySheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEntradaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PorteriaViewModel by activityViewModel()
    private var lookupJob: Job? = null

    /** Tras pulsar "Registrar", el siguiente render de errores lleva el foco al primero. */
    private var focusErrorOnRender = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Controlate_BottomSheet)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetEntradaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) viewModel.onFormOpened()
        binding.etRut.applyRutFormatter()
        binding.cbEsPadron.setOnCheckedChangeListener { _, checked -> binding.tilPadron.isVisible = checked }
        binding.tilPadron.isVisible = binding.cbEsPadron.isChecked

        val detail = arguments?.getLong(ARG_DETAIL_ID)?.takeIf { it > 0 }
            ?.let { id -> viewModel.state.value.registros.firstOrNull { it.id == id } }
        if (detail != null) showReadOnly(detail) else setUpNewEntry()
    }

    override fun onStart() {
        super.onStart()
        expandFully()
    }

    private fun setUpNewEntry() {
        binding.rbVisita.isChecked = true
        binding.etPatente.doAfterTextChanged { text ->
            viewModel.clearFieldError(ValidationError.Field.PATENTE)
            debounceLookup { viewModel.lookupPlate(text?.toString().orEmpty()) }
        }
        binding.etRut.doAfterTextChanged { text ->
            viewModel.clearFieldError(ValidationError.Field.RUT)
            val raw = text?.toString().orEmpty()
            if (Rut.isValid(raw)) debounceLookup { viewModel.lookupRut(raw) }
        }
        binding.etNombre.doAfterTextChanged { viewModel.clearFieldError(ValidationError.Field.NOMBRE) }
        binding.etPadron.doAfterTextChanged { viewModel.clearFieldError(ValidationError.Field.PADRON) }
        binding.cbEsPadron.setOnCheckedChangeListener { _, checked ->
            binding.tilPadron.isVisible = checked
            if (!checked) viewModel.clearFieldError(ValidationError.Field.PADRON)
        }
        validateOnBlur(binding.etPatente, ValidationError.Field.PATENTE)
        validateOnBlur(binding.etNombre, ValidationError.Field.NOMBRE)
        validateOnBlur(binding.etRut, ValidationError.Field.RUT)
        binding.btnGuardar.setOnClickListener {
            focusErrorOnRender = true
            viewModel.register(readInput())
        }

        collectWhenStarted(viewModel.formErrors, ::showErrors)
        collectWhenStarted(viewModel.formSaving) { saving -> binding.btnGuardar.isEnabled = !saving }
        collectWhenStarted(viewModel.formEvents) { event ->
            when (event) {
                // La confirmación la muestra la pantalla principal, anclada sobre su botón.
                PorteriaViewModel.FormEvent.Saved -> dismiss()
                is PorteriaViewModel.FormEvent.Autofill -> autofill(event.persona)
                is PorteriaViewModel.FormEvent.Failed -> {
                    focusErrorOnRender = false
                    binding.tvFormStatus.showFormStatus(event.message.resolve(requireContext()), R.color.error_red)
                }
            }
        }
    }

    private fun validateOnBlur(field: EditText, which: ValidationError.Field) {
        field.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && _binding != null) viewModel.onFieldBlur(which, field.textTrimmed())
        }
    }

    private fun debounceLookup(block: () -> Unit) {
        lookupJob?.cancel()
        lookupJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(LOOKUP_DEBOUNCE_MS)
            block()
        }
    }

    private fun readInput() = VehicleEntryInput(
        patente = binding.etPatente.textTrimmed(),
        nombre = binding.etNombre.textTrimmed(),
        rut = binding.etRut.textTrimmed(),
        departamento = binding.etDepto.textTrimmed(),
        descripcionAuto = binding.etAuto.textTrimmed(),
        empresa = binding.etEmpresa.textTrimmed(),
        tipoPersona = selectedType(),
        esPadron = binding.cbEsPadron.isChecked,
        padron = binding.etPadron.textTrimmed(),
        observaciones = null
    )

    private fun selectedType(): PersonaType = when (binding.rgTipoPersona.checkedRadioButtonId) {
        binding.rbEstudiante.id -> PersonaType.ESTUDIANTE
        binding.rbFuncionario.id -> PersonaType.FUNCIONARIO
        else -> PersonaType.VISITA
    }

    private fun typeButton(type: PersonaType): RadioButton? = when (type) {
        PersonaType.VISITA -> binding.rbVisita
        PersonaType.ESTUDIANTE -> binding.rbEstudiante
        PersonaType.FUNCIONARIO -> binding.rbFuncionario
        else -> null
    }

    private fun showErrors(errors: List<ValidationError>) {
        val byField = errors.associateBy { it.field }
        binding.tilPatente.error = byField[ValidationError.Field.PATENTE]?.let { getString(it.messageRes) }
        binding.tilNombre.error = byField[ValidationError.Field.NOMBRE]?.let { getString(it.messageRes) }
        binding.tilRut.error = byField[ValidationError.Field.RUT]?.let { getString(it.messageRes) }
        binding.tilPadron.error = byField[ValidationError.Field.PADRON]?.let { getString(it.messageRes) }
        if (focusErrorOnRender && errors.isNotEmpty()) {
            focusErrorOnRender = false
            focusFirstError(listOf(binding.tilPatente, binding.tilNombre, binding.tilRut, binding.tilPadron))
        }
    }

    /** Completa solo los campos vacíos: nunca pisa lo que el guardia ya escribió. */
    private fun autofill(persona: Persona) = with(binding) {
        if (etPatente.text.isNullOrBlank()) etPatente.setText(persona.patente)
        if (etNombre.text.isNullOrBlank()) etNombre.setText(persona.nombreApellido)
        if (etDepto.text.isNullOrBlank()) etDepto.setText(persona.departamento)
        if (etRut.text.isNullOrBlank() && persona.rut != null) etRut.setText(persona.rut)
        if (etAuto.text.isNullOrBlank()) etAuto.setText(persona.descripcionAuto)
        if (etEmpresa.text.isNullOrBlank()) etEmpresa.setText(persona.empresa)
        typeButton(PersonaType.fromCode(persona.tipoPersona))?.isChecked = true
        // Aviso en línea: un snackbar dentro del sheet tapaba el botón "Registrar".
        tvFormStatus.showFormStatus(getString(R.string.autofill_match_found), R.color.primary_light)
    }

    private fun showReadOnly(r: RegistroIngreso) = with(binding) {
        etPatente.setText(r.patente)
        etNombre.setText(r.nombreApellido)
        etDepto.setText(r.departamento)
        etRut.setText(r.rut?.let(Rut::format))
        etEmpresa.setText(r.empresa)
        etAuto.setText(r.descripcionAuto)
        cbEsPadron.isChecked = r.esPadron
        etPadron.setText(r.padron)
        typeButton(PersonaType.fromCode(r.tipoPersona))?.isChecked = true
        listOf(etPatente, etNombre, etDepto, etRut, etEmpresa, etAuto, etPadron).forEach { it.isEnabled = false }
        listOf(cbEsPadron, rbVisita, rbEstudiante, rbFuncionario).forEach { it.isEnabled = false }
        btnGuardar.isVisible = false
    }

    override fun onDestroyView() {
        lookupJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "VehicleEntrySheet"
        private const val ARG_DETAIL_ID = "detail_id"
        private const val LOOKUP_DEBOUNCE_MS = 250L

        fun forNewEntry() = VehicleEntrySheet()

        fun forDetail(registro: RegistroIngreso) = VehicleEntrySheet().apply {
            arguments = bundleOf(ARG_DETAIL_ID to registro.id)
        }
    }
}
