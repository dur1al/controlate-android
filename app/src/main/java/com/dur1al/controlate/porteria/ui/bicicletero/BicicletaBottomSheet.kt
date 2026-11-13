package com.dur1al.controlate.porteria.ui.bicicletero

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.dur1al.controlate.App
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.BottomSheetBicicletaBinding
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.session.SessionManager
import com.dur1al.controlate.porteria.domain.util.DateFormatter
import com.dur1al.controlate.porteria.utils.RutUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class BicicletaBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBicicletaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BicicleteroViewModel by sharedViewModel()
    private val bicicletaRepository: com.dur1al.controlate.porteria.data.repository.BicicletaRepository by inject()
    private val personaRepository: com.dur1al.controlate.porteria.data.repository.PersonaRepository by inject()
    private val validateUseCase: com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase by inject()
    private var rutLookupJob: Job? = null
    private val maxRutLength = 9

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Controlate_BottomSheet)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBicicletaBinding.inflate(inflater, container, false)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureSoftInputHandling()
        setupListeners()
        binding.btnGuardarBici.setOnClickListener { saveEntry() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )
    }

    private fun setupListeners() {
        binding.rgTipoPersonaBici.check(binding.rbEstudiante.id)

        binding.etRutBici.doOnTextChanged { text, _, _, _ ->
            binding.tilRutBici.error = null
            val rawInput = text.toString()
            val clean = RutUtils.cleanRut(rawInput).take(maxRutLength)
            if (rawInput != clean) {
                binding.etRutBici.setText(clean)
                binding.etRutBici.setSelection(clean.length)
                return@doOnTextChanged
            }
            if (clean.length > 5) {
                rutLookupJob?.cancel()
                rutLookupJob = lifecycleScope.launch {
                    delay(150)
                    if (RutUtils.cleanRut(binding.etRutBici.text.toString()) != clean) return@launch
                    val registro = bicicletaRepository.getUltimoPorRut(clean)
                    if (registro != null) {
                        if (binding.etNombreBici.text.isNullOrBlank()) binding.etNombreBici.setText(registro.nombre)
                        if (binding.etCarreraBici.text.isNullOrBlank()) binding.etCarreraBici.setText(registro.carrera)
                        when (registro.tipoPersona) {
                            "FUNCIONARIO" -> binding.rgTipoPersonaBici.check(binding.rbFuncionario.id)
                            "VISITA" -> binding.rgTipoPersonaBici.check(binding.rbVisita.id)
                            else -> binding.rgTipoPersonaBici.check(binding.rbEstudiante.id)
                        }
                        Snackbar.make(binding.root, R.string.autofill_match_found, Snackbar.LENGTH_SHORT).show()
                    } else {
                        val persona = personaRepository.getPersonaByRut(clean)
                        persona?.let {
                            if (binding.etNombreBici.text.isNullOrBlank()) binding.etNombreBici.setText(it.nombreApellido)
                            if (binding.etCarreraBici.text.isNullOrBlank()) binding.etCarreraBici.setText(it.departamento)
                            Snackbar.make(binding.root, R.string.autofill_match_found, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun configureSoftInputHandling() {
        val baseBottomPadding = binding.svBiciForm.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.svBiciForm) { v, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = baseBottomPadding + maxOf(imeBottom, systemBottom))
            insets
        }

        val fields = listOf(
            binding.etNombreBici,
            binding.etCarreraBici,
            binding.etObsBici
        )
        fields.forEach(::scrollIntoViewOnFocus)
        scrollIntoViewOnFocus(binding.etRutBici) { hasFocus ->
            val clean = RutUtils.cleanRut(binding.etRutBici.text.toString()).take(maxRutLength)
            if (hasFocus) {
                if (binding.etRutBici.text.toString() != clean) {
                    binding.etRutBici.setText(clean)
                    binding.etRutBici.setSelection(clean.length)
                }
            } else if (clean.isNotBlank()) {
                val formatted = RutUtils.formatRut(clean)
                if (binding.etRutBici.text.toString() != formatted) {
                    binding.etRutBici.setText(formatted)
                }
            }
        }
    }

    private fun scrollIntoViewOnFocus(field: EditText, onFocusChanged: ((Boolean) -> Unit)? = null) {
        field.setOnFocusChangeListener { view, hasFocus ->
            onFocusChanged?.invoke(hasFocus)
            if (!hasFocus) return@setOnFocusChangeListener
            binding.svBiciForm.post {
                val y = (view.top - resources.getDimensionPixelSize(R.dimen.field_gap)).coerceAtLeast(0)
                binding.svBiciForm.smoothScrollTo(0, y)
            }
        }
    }

    private fun saveEntry() {
        val rut = RutUtils.cleanRut(binding.etRutBici.text.toString()).take(maxRutLength)
        val nombre = binding.etNombreBici.text.toString().trim()
        val carrera = binding.etCarreraBici.text.toString().trim()
        val obs = binding.etObsBici.text.toString().trim()
        val tipoPersona = when (binding.rgTipoPersonaBici.checkedRadioButtonId) {
            binding.rbFuncionario.id -> "FUNCIONARIO"
            binding.rbVisita.id -> "VISITA"
            else -> "ESTUDIANTE"
        }

        binding.tilRutBici.error = null
        binding.tilNombreBici.error = null
        binding.tilCarreraBici.error = null

        val validationResult = validateUseCase.validateBicicletaEntry(
            rut = rut,
            nombre = nombre,
            carrera = carrera
        )

        if (validationResult is com.dur1al.controlate.porteria.domain.model.Result.Success && validationResult.data.isNotEmpty()) {
            validationResult.data.forEach { error ->
                when (error.field) {
                    com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase.Field.RUT -> binding.tilRutBici.error = error.message
                    com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase.Field.NOMBRE -> binding.tilNombreBici.error = error.message
                    com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase.Field.CARRERA -> binding.tilCarreraBici.error = error.message
                    else -> {}
                }
            }
            return
        }

        val session = SessionManager(requireContext())
        val controlPoint = session.getPorteriaId()

        val nuevoRegistro = RegistroBicicleta(
            shiftId = session.getShiftId(),
            rut = rut,
            nombre = nombre,
            carrera = carrera,
            tipoPersona = tipoPersona,
            controlPoint = controlPoint,
            observacionBicicleta = obs.ifBlank { null },
            fecha = DateFormatter.todayDisplayFormat(),
            horaEntrada = DateFormatter.nowTime(),
            guardiaTurno = session.getGuardiaName().ifBlank { "No registrado" },
            turnoTipo = session.getTurnoTipo()
        )

        viewModel.registrarEntrada(nuevoRegistro)
        Toast.makeText(requireContext(), R.string.entry_registered, Toast.LENGTH_SHORT).show()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rutLookupJob?.cancel()
        rutLookupJob = null
        _binding = null
    }

    companion object {
        const val TAG = "BicicletaBottomSheet"
    }
}
