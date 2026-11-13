package com.dur1al.controlate.porteria.ui.bicicletero

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityBicicleteroDashboardBinding
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.ui.common.applyAccent
import com.dur1al.controlate.porteria.ui.common.applyReadableTint
import com.dur1al.controlate.porteria.ui.common.collectWhenStarted
import com.dur1al.controlate.porteria.ui.common.color
import com.dur1al.controlate.porteria.ui.common.readableColorOn
import com.dur1al.controlate.porteria.ui.common.shareReport
import com.dur1al.controlate.porteria.ui.login.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Bitácora del bicicletero: registros del turno, búsqueda por RUT o nombre y salida con deshacer. */
class BicicleteroDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBicicleteroDashboardBinding
    private val viewModel: BicicleteroViewModel by viewModel()
    private val adapter = BicicletaAdapter(onExitClick = { viewModel.markExit(it) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBicicleteroDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.rvBicicletas.layoutManager = LinearLayoutManager(this)
        binding.rvBicicletas.adapter = adapter
        binding.etSearch.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.btnNuevo.setOnClickListener { BikeEntrySheet().show(supportFragmentManager, BikeEntrySheet.TAG) }
        binding.btnEndShift.setOnClickListener { confirmEndShift() }

        collectWhenStarted(viewModel.state) { state ->
            state.turno?.let { turno -> applyPoint(state.point ?: fallbackPoint(turno.controlPoint)) }
            adapter.submitList(state.registros)
            val empty = !state.loading && state.registros.isEmpty()
            binding.emptyStateLayout.isVisible = empty
            binding.rvBicicletas.isVisible = !empty
            binding.btnEndShift.isEnabled = !state.busy
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                BicicleteroViewModel.Event.NoActiveShift, BicicleteroViewModel.Event.ShiftEnded -> goToLogin()
                is BicicleteroViewModel.Event.ExitMarked ->
                    Snackbar.make(binding.root, getString(R.string.exit_registered_for, event.nombre), Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.bottomBarCard)
                        .setAction(R.string.action_undo) { viewModel.undoExit(event.id, event.at) }
                        .show()
                is BicicleteroViewModel.Event.Share ->
                    if (!shareReport(event.file)) {
                        Snackbar.make(binding.root, R.string.share_report_unavailable, Snackbar.LENGTH_LONG).setAnchorView(binding.bottomBarCard).show()
                    }
                is BicicleteroViewModel.Event.Message ->
                    Snackbar.make(binding.root, event.message.resolve(this), Snackbar.LENGTH_SHORT).setAnchorView(binding.bottomBarCard).show()
            }
        }
    }

    private var themedPoint: ControlPoint? = null

    /** Turno heredado cuyo punto ya no existe: se muestra con el primer color de bicicleteros. */
    private fun fallbackPoint(id: String) =
        ControlPoint(id, getString(R.string.unknown_control_point, id), ControlPoint.Kind.BICICLETERO, 0)
    private var toolbarForeground: Int? = null

    /** Color del punto en cabecera, toolbar y botón principal; texto e iconos se eligen por contraste. */
    private fun applyPoint(point: ControlPoint) {
        if (point == themedPoint) return
        themedPoint = point
        val color = point.color(this)
        val foreground = binding.tvHeaderTitle.readableColorOn(color)
        binding.headerLayout.setBackgroundColor(color)
        binding.tvHeaderTitle.setTextColor(foreground)
        binding.btnEndShift.applyReadableTint(color)
        binding.toolbar.setBackgroundColor(color)
        binding.toolbar.setTitleTextColor(foreground)
        binding.toolbar.setSubtitleTextColor(foreground)
        binding.toolbar.subtitle = point.name
        binding.btnNuevo.applyAccent(color)
        toolbarForeground = foreground
        invalidateOptionsMenu()
    }

    private fun confirmEndShift() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.end_shift_title)
            .setMessage(R.string.end_shift_bike_message)
            .setPositiveButton(R.string.dialog_yes_finish) { _, _ -> viewModel.endShift() }
            .setNegativeButton(R.string.dialog_no, null)
            .show()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bicicletero, menu)
        toolbarForeground?.let { color ->
            menu.findItem(R.id.action_exportar)?.icon?.let { icon ->
                DrawableCompat.setTint(DrawableCompat.wrap(icon).mutate(), color)
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_exportar) {
            viewModel.exportCurrentShift()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun intent(context: Context) = Intent(context, BicicleteroDashboardActivity::class.java)
    }
}
