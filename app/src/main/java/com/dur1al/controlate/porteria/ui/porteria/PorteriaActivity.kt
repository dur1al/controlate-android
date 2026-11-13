package com.dur1al.controlate.porteria.ui.porteria

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityMainBinding
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.ui.common.applyAccent
import com.dur1al.controlate.porteria.ui.common.applyReadableTint
import com.dur1al.controlate.porteria.ui.common.collectWhenStarted
import com.dur1al.controlate.porteria.ui.common.color
import com.dur1al.controlate.porteria.ui.common.readableColorOn
import com.dur1al.controlate.porteria.ui.login.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Portería vehicular: lista del turno, búsqueda, nuevo ingreso, salida con deshacer y cierre. */
class PorteriaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: PorteriaViewModel by viewModel()

    private val adapter = RegistroAdapter(
        onExitClick = { viewModel.markExit(it) },
        onItemClick = { VehicleEntrySheet.forDetail(it).show(supportFragmentManager, VehicleEntrySheet.TAG) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvActivos.layoutManager = LinearLayoutManager(this)
        binding.rvActivos.adapter = adapter
        binding.etSearch.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.fabNuevoRedesign.setOnClickListener {
            VehicleEntrySheet.forNewEntry().show(supportFragmentManager, VehicleEntrySheet.TAG)
        }
        binding.btnEndShift.setOnClickListener { confirmEndShift() }

        observeState()
        observeEvents()
    }

    private fun observeState() = collectWhenStarted(viewModel.state) { state ->
        state.turno?.let { turno -> applyPointTheme(state.point ?: fallbackPoint(turno.controlPoint)) }
        adapter.submitList(state.registros)
        val empty = !state.loading && state.registros.isEmpty()
        binding.emptyStateLayout.isVisible = empty
        binding.rvActivos.isVisible = !empty
        binding.btnEndShift.isEnabled = !state.busy
    }

    private fun observeEvents() = collectWhenStarted(viewModel.events) { event ->
        when (event) {
            PorteriaViewModel.Event.NoActiveShift, PorteriaViewModel.Event.ShiftEnded -> goToLogin()
            is PorteriaViewModel.Event.ExitMarked ->
                Snackbar.make(binding.root, getString(R.string.exit_registered_for, event.patente), Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.bottomBarCard)
                    .setAction(R.string.action_undo) { viewModel.undoExit(event.id, event.at) }
                    .show()
            is PorteriaViewModel.Event.Message ->
                Snackbar.make(binding.root, event.message.resolve(this), Snackbar.LENGTH_SHORT).setAnchorView(binding.bottomBarCard).show()
        }
    }

    private var themedPoint: ControlPoint? = null

    /** Turno heredado cuyo punto ya no existe: se muestra con el primer color de porterías. */
    private fun fallbackPoint(id: String) =
        ControlPoint(id, getString(R.string.unknown_control_point, id), ControlPoint.Kind.PORTERIA, 0)

    /** Color del punto en cabecera y botón principal; texto e iconos se eligen por contraste. */
    private fun applyPointTheme(point: ControlPoint) {
        if (point == themedPoint) return
        themedPoint = point
        val color = point.color(this)
        binding.headerLayout.setBackgroundColor(color)
        binding.tvHeaderTitle.setTextColor(binding.tvHeaderTitle.readableColorOn(color))
        binding.btnEndShift.applyReadableTint(color)
        binding.fabNuevoRedesign.applyAccent(color)
    }

    private fun confirmEndShift() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.end_shift_title)
            .setMessage(R.string.end_shift_message)
            .setPositiveButton(R.string.dialog_yes_finish) { _, _ -> viewModel.endShift() }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    companion object {
        fun intent(context: Context) = Intent(context, PorteriaActivity::class.java)
    }
}
