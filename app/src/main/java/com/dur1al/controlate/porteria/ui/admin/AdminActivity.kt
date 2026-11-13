package com.dur1al.controlate.porteria.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityAdminBinding
import com.dur1al.controlate.databinding.ItemPointStatBinding
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.PointStat
import com.dur1al.controlate.porteria.ui.common.collectWhenStarted
import com.dur1al.controlate.porteria.ui.common.labelRes
import com.dur1al.controlate.porteria.ui.common.shareReport
import com.dur1al.controlate.porteria.ui.db.DatabaseManagerActivity
import com.dur1al.controlate.porteria.ui.settings.SettingsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.NumberFormat

/** Panel administrador: resumen del día por punto, reportes, directorio, ajustes y limpieza. */
class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val viewModel: AdminViewModel by viewModel()
    private val numberFormat = NumberFormat.getIntegerInstance()

    /** Opciones del filtro: `null` = general, luego cada punto de control. */
    private val scopes: List<ControlPoint?> = listOf(null) + ControlPoint.entries

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        val labels = scopes.map { it?.let { p -> getString(p.labelRes) } ?: getString(R.string.admin_scope_general) }
        binding.actvScope.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, labels))
        binding.actvScope.setText(labels.first(), false)
        binding.actvScope.setOnItemClickListener { _, _, position, _ -> viewModel.selectScope(scopes[position]) }

        binding.cardReports.setOnClickListener { startActivity(Intent(this, ShiftReportActivity::class.java)) }
        binding.cardDirectory.setOnClickListener { startActivity(Intent(this, DatabaseManagerActivity::class.java)) }
        binding.cardSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.btnExportAdminSummary.setOnClickListener { viewModel.exportSummary() }
        binding.btnLimpiarTodo.setOnClickListener { confirmClear() }

        collectWhenStarted(viewModel.state) { state ->
            binding.tvEntraronCount.text = numberFormat.format(state.totals.entradas)
            binding.tvSalieronCount.text = numberFormat.format(state.totals.salidas)
            binding.tvDentroCount.text = numberFormat.format(state.totals.dentro)
            binding.btnExportAdminSummary.isEnabled = !state.busy
            renderPoints(state.points)
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is AdminViewModel.Event.Share ->
                    if (!shareReport(event.file)) {
                        Snackbar.make(binding.root, R.string.share_report_unavailable, Snackbar.LENGTH_LONG).show()
                    }
                AdminViewModel.Event.Cleared ->
                    Snackbar.make(binding.root, R.string.admin_delete_operational_success, Snackbar.LENGTH_SHORT).show()
                is AdminViewModel.Event.Message ->
                    Snackbar.make(binding.root, event.message.resolve(this), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun renderPoints(points: List<PointStat>) {
        binding.llPointStats.removeAllViews()
        points.forEach { stat ->
            val item = ItemPointStatBinding.inflate(layoutInflater, binding.llPointStats, false)
            item.tvPointName.setText(stat.controlPoint.labelRes)
            item.tvPointStats.text = getString(R.string.admin_point_stats_format, stat.entradas, stat.salidas, stat.dentro)
            item.tvPointStats.setTextColor(
                ContextCompat.getColor(this, if (stat.dentro > 0) R.color.bicicletero2_primary else R.color.text_hint)
            )
            binding.llPointStats.addView(item.root)
        }
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.admin_delete_operational_title)
            .setMessage(R.string.admin_delete_operational_message)
            .setPositiveButton(R.string.dialog_confirm) { _, _ -> viewModel.clearOperationalData() }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
