package com.dur1al.controlate.porteria.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.BottomSheetEntradaBinding
import com.dur1al.controlate.porteria.data.local.session.SessionManager
import com.dur1al.controlate.porteria.utils.RutUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class EntradaBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEntradaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by sharedViewModel()
    private val validateUseCase: com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase by inject()
    private lateinit var sessionManager: SessionManager
    private var rutLookupJob: Job? = null
    private val maxRutLength = 9

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Controlate_BottomSheet)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetEntradaBinding.inflate(inflater, container, false)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        configureSoftInputHandling()

        setupListeners()

        binding.cbEsPadron.setOnCheckedChangeListener { _, isChecked ->
            binding.tilPadron.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) binding.tilPadron.error = null
        }

        viewModel.registroToEdit.observe(viewLifecycleOwner) { registro ->
            if (registro != null) {
                fillData(registro.patente, registro.nombreApellido, registro.departamento, registro.rut, registro.padron, registro.esPadron, registro.descripcionAuto, registro.empresa, registro.tipoPersona)
                binding.btnGuardar.visibility = View.GONE
            }
        }

        viewModel.personaFound.observe(viewLifecycleOwner) { persona ->
            persona?.let {
                fillData(it.patente, it.nombreApellido, it.departamento, it.rut, it.padron, false, it.descripcionAuto, it.empresa, it.tipoPersona, isAutofill = true)
            }
        }

        binding.btnGuardar.setOnClickListener { saveEntry() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )
    }

    private fun setupListeners() {
        binding.etPatente.doOnTextChanged { text, _, _, _ ->
            val input = text.toString().uppercase()
            if (text.toString() != input) {
                binding.etPatente.setText(input)
                binding.etPatente.setSelection(input.length)
                return@doOnTextChanged
            }

            val plateClean = input.replace(" ", "")
            binding.tilPatente.error = null
            if (plateClean.length >= 4 && viewModel.registroToEdit.value == null) {
                searchByPlate(plateClean)
            }
        }

        binding.etNombre.doOnTextChanged { _, _, _, _ -> binding.tilNombre.error = null }
        binding.etPadron.doOnTextChanged { _, _, _, _ -> binding.tilPadron.error = null }

        binding.etRut.doOnTextChanged { text, _, _, _ ->
            val rawInput = text.toString()
            val clean = RutUtils.cleanRut(rawInput).take(maxRutLength)
            if (rawInput != clean) {
                binding.etRut.setText(clean)
                binding.etRut.setSelection(clean.length)
                return@doOnTextChanged
            }
            if (clean.length >= 7) {
                rutLookupJob?.cancel()
                rutLookupJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(150)
                    if (RutUtils.cleanRut(binding.etRut.text.toString()) == clean) {
                        viewModel.searchByRut(clean)
                    }
                }
            }
        }
    }

    private fun configureSoftInputHandling() {
        val baseBottomPadding = binding.svEntradaForm.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.svEntradaForm) { v, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = baseBottomPadding + maxOf(imeBottom, systemBottom))
            insets
        }

        val fields = listOf(
            binding.etPatente,
            binding.etNombre,
            binding.etDepto,
            binding.etEmpresa,
            binding.etPadron,
            binding.etAuto
        )
        fields.forEach(::scrollIntoViewOnFocus)
        scrollIntoViewOnFocus(binding.etRut) { hasFocus ->
            val clean = RutUtils.cleanRut(binding.etRut.text.toString()).take(maxRutLength)
            if (hasFocus) {
                if (binding.etRut.text.toString() != clean) {
                    binding.etRut.setText(clean)
                    binding.etRut.setSelection(clean.length)
                }
            } else if (clean.isNotBlank()) {
                val formatted = RutUtils.formatRut(clean)
                if (binding.etRut.text.toString() != formatted) {
                    binding.etRut.setText(formatted)
                }
            }
        }
    }

    private fun scrollIntoViewOnFocus(field: EditText, onFocusChanged: ((Boolean) -> Unit)? = null) {
        field.setOnFocusChangeListener { view, hasFocus ->
            onFocusChanged?.invoke(hasFocus)
            if (!hasFocus) return@setOnFocusChangeListener
            binding.svEntradaForm.post {
                val y = (view.top - resources.getDimensionPixelSize(R.dimen.field_gap)).coerceAtLeast(0)
                binding.svEntradaForm.smoothScrollTo(0, y)
            }
        }
    }

    private fun searchByPlate(plate: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val persona = viewModel.getPersonaByPlate(plate)
            persona?.let {
                fillData(it.patente, it.nombreApellido, it.departamento, it.rut, it.padron, false, it.descripcionAuto, it.empresa, it.tipoPersona, isAutofill = true)
            }
        }
    }

    private fun fillData(plate: String, nombre: String, depto: String, rut: String?, padron: String?, esPadron: Boolean, auto: String, empresa: String?, tipo: String?, isAutofill: Boolean = false) {
        binding.apply {
            if (etPatente.text.isNullOrBlank()) etPatente.setText(plate)
            if (etNombre.text.isNullOrBlank()) etNombre.setText(nombre)
            if (etDepto.text.isNullOrBlank()) etDepto.setText(depto)
            if (etRut.text.isNullOrBlank() && rut != null) etRut.setText(RutUtils.formatRut(rut))

            cbEsPadron.isChecked = esPadron
            if (padron != null && etPadron.text.isNullOrBlank()) etPadron.setText(padron)

            if (etAuto.text.isNullOrBlank()) etAuto.setText(auto)
            if (etEmpresa.text.isNullOrBlank()) etEmpresa.setText(empresa)

            when (tipo) {
                "Visita" -> rbVisita.isChecked = true
                "Estudiante" -> rbEstudiante.isChecked = true
                "Funcionario" -> rbFuncionario.isChecked = true
            }
        }
        if (isAutofill) {
            Snackbar.make(binding.root, R.string.autofill_match_found, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun saveEntry() {
        val plate = binding.etPatente.text.toString().trim().uppercase().replace(" ", "")
        val nombre = binding.etNombre.text.toString().trim()
        val rutRaw = RutUtils.cleanRut(binding.etRut.text.toString()).take(maxRutLength)

        clearValidationErrors()

        val depto = binding.etDepto.text.toString().trim()
        val empresa = binding.etEmpresa.text.toString().trim()
        val esPadron = binding.cbEsPadron.isChecked
        val padron = if (esPadron) binding.etPadron.text.toString().trim() else ""
        val autoDesc = binding.etAuto.text.toString().trim()

        val tipo = when (binding.rgTipoPersona.checkedRadioButtonId) {
            binding.rbVisita.id -> "Visita"
            binding.rbEstudiante.id -> "Estudiante"
            binding.rbFuncionario.id -> "Funcionario"
            else -> "Visita"
        }

        val validationResult = validateUseCase.validatePorteriaEntry(
            patente = plate,
            nombre = nombre,
            rut = rutRaw,
            esPadron = esPadron,
            padron = padron
        )

        if (validationResult is com.dur1al.controlate.porteria.domain.model.Result.Success && validationResult.data.isNotEmpty()) {
            validationResult.data.forEach { error ->
                when (error.field) {
                    com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase.Field.PATENTE -> binding.tilPatente.error = error.message
                    com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase.Field.NOMBRE -> binding.tilNombre.error = error.message
                    com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase.Field.RUT -> binding.tilRut.error = error.message
                    com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase.Field.PADRON -> binding.tilPadron.error = error.message
                    else -> {}
                }
            }
            return
        }

        viewModel.saveEntry(
            porteria = sessionManager.getPorteriaId(),
            porteriaName = sessionManager.getPorteriaName(),
            guardia = sessionManager.getGuardiaName(),
            fechaTurno = sessionManager.getFechaTurno(),
            turnoTipo = sessionManager.getTurnoTipo(),
            shiftId = sessionManager.getShiftId(),
            patente = plate,
            nombre = nombre,
            padron = padron.ifBlank { null },
            esPadron = esPadron,
            depto = depto,
            rut = rutRaw.ifBlank { null },
            autoDesc = autoDesc,
            empresa = empresa.ifBlank { null },
            tipoPersona = tipo
        )

        viewModel.setRegistroToEdit(null)
        viewModel.clearPersonaFound()
        dismiss()
    }

    private fun clearValidationErrors() {
        binding.tilPatente.error = null
        binding.tilNombre.error = null
        binding.tilPadron.error = null
        binding.tilRut.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rutLookupJob?.cancel()
        rutLookupJob = null
        _binding = null
    }

    companion object {
        const val TAG = "EntradaBottomSheet"
    }
}
