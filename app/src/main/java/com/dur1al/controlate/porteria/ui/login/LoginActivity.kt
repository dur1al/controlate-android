package com.dur1al.controlate.porteria.ui.login

import android.content.Intent
import android.content.res.ColorStateList
import com.dur1al.controlate.BuildConfig
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.snackbar.Snackbar
import com.dur1al.controlate.MainActivity
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityLoginBinding
import com.dur1al.controlate.porteria.data.ControlPoint
import com.dur1al.controlate.porteria.data.local.session.SessionManager
import com.dur1al.controlate.porteria.ui.admin.AdminActivity
import com.dur1al.controlate.porteria.ui.bicicletero.BicicleteroDashboardActivity
import com.dur1al.controlate.porteria.domain.util.DateFormatter
import com.dur1al.controlate.porteria.ui.examen.ExamenActivity

import org.koin.android.ext.android.inject

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    private enum class ControlType { PORTERIA, BICICLETERO }
    private enum class ShiftType(val value: String) {
        DAY("DIA (08:00 - 20:00)"),
        NIGHT("NOCHE (20:00 - 08:00)")
    }

    private data class PointOption(
        val label: String,
        val controlPoint: ControlPoint
    )

    private val porteriaOptions by lazy {
        listOf(
            PointOption(getString(R.string.porteria_2), ControlPoint.PORTERIA_2),
            PointOption(getString(R.string.porteria_3), ControlPoint.PORTERIA_3),
            PointOption(getString(R.string.porteria_4), ControlPoint.PORTERIA_4)
        )
    }

    private val bicicleteroOptions by lazy {
        listOf(
            PointOption(getString(R.string.bicicletero_1), ControlPoint.BICICLETERO_1),
            PointOption(getString(R.string.bicicletero_2), ControlPoint.BICICLETERO_2)
        )
    }

    private var selectedControlType = ControlType.PORTERIA
    private var selectedShiftType = ShiftType.DAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        if (sessionManager.isShiftActive()) {
            navigateToDashboardForActiveShift()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        val dateForDisplay = DateFormatter.todayDisplayFormat()
        binding.tvFechaActual.text = getString(R.string.login_shift_date, dateForDisplay)

        setupToggleListeners()
        binding.toggleShiftType.check(binding.btnShiftDay.id)
        binding.toggleControlType.check(binding.btnControlPorteria.id)

        applyControlTheme(ControlType.PORTERIA)
        updateControlPointOptions(ControlType.PORTERIA)

        binding.btnIniciarTurno.setOnClickListener { startShiftIfValid() }
        binding.btnAdminAccess.setOnClickListener { showAdminPasswordDialog() }
        binding.btnExamenOS10.setOnClickListener {
            startActivity(Intent(this, ExamenActivity::class.java))
        }
        binding.btnAbout.setOnClickListener { showAboutDialog() }
        binding.etGuardia.doOnTextChanged { _, _, _, _ -> binding.tilGuardia.error = null }
    }

    private fun setupToggleListeners() {
        binding.toggleShiftType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedShiftType = if (checkedId == binding.btnShiftNight.id) {
                ShiftType.NIGHT
            } else {
                ShiftType.DAY
            }
            updateShiftButtonsStyle()
        }

        binding.toggleControlType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedControlType = if (checkedId == binding.btnControlBicicletero.id) {
                ControlType.BICICLETERO
            } else {
                ControlType.PORTERIA
            }
            applyControlTheme(selectedControlType)
            updateControlPointOptions(selectedControlType)
        }
    }

    private fun updateShiftButtonsStyle() {
        val selectedColor = if (selectedShiftType == ShiftType.DAY) {
            ContextCompat.getColor(this, R.color.warning_orange)
        } else {
            ContextCompat.getColor(this, R.color.primary_light)
        }
        binding.btnShiftDay.strokeColor = ColorStateList.valueOf(selectedColor)
        binding.btnShiftNight.strokeColor = ColorStateList.valueOf(selectedColor)
    }

    private fun applyControlTheme(type: ControlType) {
        val primaryColor = if (type == ControlType.BICICLETERO) {
            ContextCompat.getColor(this, R.color.bicicletero2_primary)
        } else {
            ContextCompat.getColor(this, R.color.bicicletero1_primary)
        }
        val surfaceColor = if (type == ControlType.BICICLETERO) {
            ContextCompat.getColor(this, R.color.surface_bicicletero)
        } else {
            ContextCompat.getColor(this, R.color.black)
        }

        binding.loginRoot.setBackgroundColor(surfaceColor)
        binding.btnIniciarTurno.backgroundTintList = ColorStateList.valueOf(primaryColor)
        binding.btnAdminAccess.setTextColor(primaryColor)
    }

    private fun updateControlPointOptions(type: ControlType) {
        val labels = when (type) {
            ControlType.PORTERIA -> porteriaOptions.map { it.label }
            ControlType.BICICLETERO -> bicicleteroOptions.map { it.label }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, labels)
        binding.actvControlPoint.setAdapter(adapter)
        binding.actvControlPoint.setText(labels.first(), false)
    }

    private fun startShiftIfValid() {
        val guardia = binding.etGuardia.text?.toString().orEmpty().trim()
        val selectedPointLabel = binding.actvControlPoint.text?.toString().orEmpty().trim()

        if (guardia.length < 3) {
            binding.tilGuardia.error = getString(R.string.login_guard_name_error)
            return
        }
        binding.tilGuardia.error = null

        val pointOption = resolvePointOption(selectedPointLabel)
        if (pointOption == null) {
            Snackbar.make(binding.root, R.string.login_control_point_error, Snackbar.LENGTH_SHORT).show()
            return
        }

        val dateForDb = DateFormatter.todayDbFormat()
        sessionManager.startShift(
            guardia,
            pointOption.label,
            pointOption.controlPoint.id,
            dateForDb,
            selectedShiftType.value
        )

        val intent = when (pointOption.controlPoint) {
            ControlPoint.BICICLETERO_1, ControlPoint.BICICLETERO_2 ->
                Intent(this, BicicleteroDashboardActivity::class.java)
            else -> Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_CONTROL_POINT_NAME, pointOption.controlPoint.name)
            }
        }
        startActivity(intent)
        finish()
    }

    private fun resolvePointOption(label: String): PointOption? {
        val options = if (selectedControlType == ControlType.BICICLETERO) {
            bicicleteroOptions
        } else {
            porteriaOptions
        }
        return options.firstOrNull { it.label.equals(label, ignoreCase = true) }
    }

    private fun navigateToDashboardForActiveShift() {
        val pointId = sessionManager.getPorteriaId()
        val controlPoint = ControlPoint.fromId(pointId)
        val intent = when (controlPoint) {
            ControlPoint.BICICLETERO_1, ControlPoint.BICICLETERO_2 ->
                Intent(this, BicicleteroDashboardActivity::class.java)
            else -> Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_CONTROL_POINT_NAME, controlPoint.name)
            }
        }
        startActivity(intent)
        finish()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.login_about_title)
            .setMessage(R.string.login_about_message)
            .setPositiveButton(R.string.dialog_close, null)
            .show()
    }

    private val securityManager: com.dur1al.controlate.porteria.data.local.security.SecurityManager by inject()

    private fun showAdminPasswordDialog() {
        if (!securityManager.isAdminPinConfigured()) {
            showAdminPinSetupDialog()
            return
        }
        if (securityManager.isLockedOut()) {
            val remaining = securityManager.getRemainingLockoutSeconds()
            Snackbar.make(
                binding.root,
                getString(R.string.login_admin_locked_out, remaining),
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = getString(R.string.login_admin_password_hint)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.login_admin_title)
            .setView(input)
            .setPositiveButton(R.string.login_admin_enter) { _, _ ->
                if (securityManager.validateAdminPin(input.text.toString())) {
                    startActivity(Intent(this, AdminActivity::class.java))
                } else if (securityManager.isLockedOut()) {
                    val remaining = securityManager.getRemainingLockoutSeconds()
                    Snackbar.make(
                        binding.root,
                        getString(R.string.login_admin_locked_out, remaining),
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(binding.root, R.string.login_admin_wrong_password, Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showAdminPinSetupDialog() {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
        }
        val pinInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = getString(R.string.login_admin_password_hint)
        }
        val confirmInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = getString(R.string.login_admin_pin_confirm_hint)
        }
        container.addView(pinInput)
        container.addView(confirmInput)

        AlertDialog.Builder(this)
            .setTitle(R.string.login_admin_setup_title)
            .setMessage(R.string.login_admin_setup_message)
            .setView(container)
            .setPositiveButton(R.string.login_admin_setup_save) { _, _ ->
                val pin = pinInput.text.toString()
                val confirm = confirmInput.text.toString()
                when {
                    pin != confirm -> {
                        Snackbar.make(binding.root, R.string.login_admin_pin_mismatch, Snackbar.LENGTH_SHORT).show()
                    }
                    !securityManager.setAdminPin(pin) -> {
                        Snackbar.make(binding.root, R.string.login_admin_pin_invalid_format, Snackbar.LENGTH_SHORT).show()
                    }
                    else -> {
                        startActivity(Intent(this, AdminActivity::class.java))
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
