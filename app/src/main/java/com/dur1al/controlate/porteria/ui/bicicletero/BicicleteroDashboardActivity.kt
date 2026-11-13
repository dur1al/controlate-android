package com.dur1al.controlate.porteria.ui.bicicletero

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityBicicleteroDashboardBinding
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.ui.common.collectWhenStarted
import com.dur1al.controlate.porteria.ui.common.colorRes
import com.dur1al.controlate.porteria.ui.common.labelRes
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
            state.turno?.let { applyPoint(ControlPoint.fromId(it.controlPoint) ?: ControlPoint.BICICLETERO_1) }
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
                        .setAction(R.string.action_undo) { viewModel.undoExit(event.id, event.at) }
                        .show()
                is BicicleteroViewModel.Event.Share ->
                    if (!shareReport(event.file)) {
                        Snackbar.make(binding.root, R.string.share_report_unavailable, Snackbar.LENGTH_LONG).show()
                    }
                is BicicleteroViewModel.Event.Message ->
                    Snackbar.make(binding.root, event.message.resolve(this), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyPoint(point: ControlPoint) {
        val color = ContextCompat.getColor(this, point.colorRes)
        binding.headerLayout.setBackgroundColor(color)
        binding.toolbar.setBackgroundColor(color)
        binding.toolbar.subtitle = getString(point.labelRes)
        binding.btnNuevo.backgroundTintList = ColorStateList.valueOf(color)
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
