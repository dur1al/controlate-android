package com.dur1al.controlate.porteria.ui.login

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityLoginBinding
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.ShiftType
import com.dur1al.controlate.porteria.domain.time.Clock
import com.dur1al.controlate.porteria.domain.time.TimeFormat
import com.dur1al.controlate.porteria.ui.admin.AdminActivity
import com.dur1al.controlate.porteria.ui.bicicletero.BicicleteroDashboardActivity
import com.dur1al.controlate.porteria.ui.common.applyAccent
import com.dur1al.controlate.porteria.ui.common.collectWhenStarted
import com.dur1al.controlate.porteria.ui.common.openUrl
import com.dur1al.controlate.porteria.ui.common.textTrimmed
import com.dur1al.controlate.porteria.ui.porteria.PorteriaActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Punto de entrada: inicio de turno, acceso administrador e información de contacto. */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModel()
    private val clock: Clock by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvFechaActual.text = getString(R.string.login_shift_date, TimeFormat.displayDate(clock.now()))
        binding.toggleShiftType.check(binding.btnShiftDay.id)
        binding.toggleControlType.check(binding.btnControlPorteria.id)
        setUpListeners()

        collectWhenStarted(viewModel.state, ::render)
        collectWhenStarted(viewModel.events, ::handle)
    }

    private fun setUpListeners() = with(binding) {
        toggleShiftType.addOnButtonCheckedListener { _, id, checked ->
            if (checked) viewModel.selectShiftType(if (id == btnShiftNight.id) ShiftType.NIGHT else ShiftType.DAY)
        }
        toggleControlType.addOnButtonCheckedListener { _, id, checked ->
            if (checked) {
                viewModel.selectKind(
                    if (id == btnControlBicicletero.id) ControlPoint.Kind.BICICLETERO else ControlPoint.Kind.PORTERIA
                )
            }
        }
        actvControlPoint.setOnItemClickListener { _, _, position, _ ->
            viewModel.state.value.points.getOrNull(position)?.let(viewModel::selectPoint)
        }
        etGuardia.doAfterTextChanged { viewModel.clearGuardNameError() }
        btnIniciarTurno.setOnClickListener { viewModel.startShift(etGuardia.textTrimmed()) }
        btnAdminAccess.setOnClickListener { viewModel.onAdminClicked() }
        btnAbout.setOnClickListener { showAboutDialog() }
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.login_about_title)
            .setMessage(getString(R.string.login_about_message, getString(R.string.developer_contact_display)))
            .setPositiveButton(R.string.login_about_open_github) { _, _ -> openContactPage() }
            .setNegativeButton(R.string.dialog_close, null)
            .show()
    }

    /** Abre el perfil del desarrollador; sin navegador instalado se informa en lugar de fallar. */
    private fun openContactPage() {
        if (!openUrl(getString(R.string.developer_contact_url))) {
            Snackbar.make(binding.root, R.string.error_no_browser, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun render(state: LoginViewModel.UiState) = with(binding) {
        loginRoot.isVisible = !state.checkingSession
        tilGuardia.error = if (state.guardNameError) getString(R.string.login_guard_name_error) else null

        actvControlPoint.setAdapter(
            ArrayAdapter(this@LoginActivity, android.R.layout.simple_dropdown_item_1line, state.points.map { it.name })
        )
        actvControlPoint.setText(state.selectedPoint?.name.orEmpty(), false)
        // Sin puntos activos del tipo elegido: se explica en el campo y no se puede iniciar turno.
        tilControlPoint.error = if (state.points.isEmpty() && !state.checkingSession) getString(R.string.login_no_active_points) else null
        btnIniciarTurno.isEnabled = !state.busy && state.selectedPoint != null

        val isBike = state.kind == ControlPoint.Kind.BICICLETERO
        val primary = ContextCompat.getColor(this@LoginActivity, if (isBike) R.color.action_bike else R.color.action_primary)
        // El texto del botón de admin va sobre el fondo de la pantalla: usa la variante clara del acento.
        val accentText = ContextCompat.getColor(this@LoginActivity, if (isBike) R.color.bicicletero2_primary else R.color.primary_light)
        loginRoot.setBackgroundColor(
            ContextCompat.getColor(this@LoginActivity, if (isBike) R.color.surface_bicicletero else R.color.black)
        )
        btnIniciarTurno.applyAccent(primary)
        btnAdminAccess.setTextColor(accentText)

        val shiftColor = ContextCompat.getColor(
            this@LoginActivity, if (state.shiftType == ShiftType.DAY) R.color.warning_orange else R.color.primary_light
        )
        btnShiftDay.strokeColor = ColorStateList.valueOf(shiftColor)
        btnShiftNight.strokeColor = ColorStateList.valueOf(shiftColor)
    }

    private fun handle(event: LoginViewModel.Event) {
        when (event) {
            is LoginViewModel.Event.OpenShift -> {
                val intent = when (event.kind) {
                    ControlPoint.Kind.BICICLETERO -> BicicleteroDashboardActivity.intent(this)
                    ControlPoint.Kind.PORTERIA -> PorteriaActivity.intent(this)
                }
                startActivity(intent)
                finish()
            }
            LoginViewModel.Event.OpenAdmin -> startActivity(Intent(this, AdminActivity::class.java))
            LoginViewModel.Event.AskPin -> showPinDialog()
            LoginViewModel.Event.AskPinSetup -> showPinSetupDialog()
            is LoginViewModel.Event.Message ->
                Snackbar.make(binding.root, event.message.resolve(this), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun pinField(hintRes: Int) = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        hint = getString(hintRes)
    }

    private fun dialogContainer(vararg fields: EditText) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val padding = resources.getDimensionPixelSize(R.dimen.screen_padding)
        setPadding(padding, padding / 2, padding, 0)
        fields.forEach(::addView)
    }

    private fun showPinDialog() {
        val pin = pinField(R.string.login_admin_password_hint)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.login_admin_title)
            .setView(dialogContainer(pin))
            .setPositiveButton(R.string.login_admin_enter) { _, _ -> viewModel.submitPin(pin.text.toString()) }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showPinSetupDialog() {
        val pin = pinField(R.string.login_admin_password_hint)
        val confirmation = pinField(R.string.login_admin_pin_confirm_hint)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.login_admin_setup_title)
            .setMessage(R.string.login_admin_setup_message)
            .setView(dialogContainer(pin, confirmation))
            .setPositiveButton(R.string.login_admin_setup_save) { _, _ ->
                viewModel.setupPin(pin.text.toString(), confirmation.text.toString())
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
