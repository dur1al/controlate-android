package com.dur1al.controlate.porteria.ui.admin

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.dur1al.controlate.App
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityAdminBinding
import com.dur1al.controlate.databinding.ItemPointStatBinding
import com.dur1al.controlate.porteria.data.ControlPoint
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.domain.util.DateFormatter
import com.dur1al.controlate.porteria.ui.dashboard.DashboardViewModel
import com.dur1al.controlate.porteria.ui.db.DatabaseManagerActivity
import com.dur1al.controlate.porteria.utils.ExportResult
import com.dur1al.controlate.porteria.utils.ReportManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class AdminActivity : AppCompatActivity() {

    private enum class ScopeOption(val labelRes: Int, val pointId: String) {
        GENERAL(R.string.admin_dashboard_scope_hint, "General"),
        PORTERIA_2(R.string.porteria_2, ControlPoint.PORTERIA_2.id),
        PORTERIA_3(R.string.porteria_3, ControlPoint.PORTERIA_3.id),
        PORTERIA_4(R.string.porteria_4, ControlPoint.PORTERIA_4.id),
        BICI_1(R.string.bicicletero_1, ControlPoint.BICICLETERO_1.id),
        BICI_2(R.string.bicicletero_2, ControlPoint.BICICLETERO_2.id)
    }

    private lateinit var binding: ActivityAdminBinding
    private lateinit var reportManager: ReportManager

    private val registroRepository: com.dur1al.controlate.porteria.data.repository.RegistroRepository by inject()
    private val bicicletaRepository: com.dur1al.controlate.porteria.data.repository.BicicletaRepository by inject()

    private val viewModel: DashboardViewModel by viewModel()

    private var selectedScope: ScopeOption = ScopeOption.GENERAL
    private var pointStats: List<ReportManager.SummaryPointRow> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reportManager = ReportManager(this)
        setupToolbar()
        setupScopeFilter()
        loadStats()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupScopeFilter() {
        val labels = ScopeOption.entries.map { getString(it.labelRes) }
        binding.actvScope.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, labels))
        binding.actvScope.setText(getString(ScopeOption.GENERAL.labelRes), false)
        binding.actvScope.setOnItemClickListener { _, _, position, _ ->
            selectedScope = ScopeOption.entries[position]
            loadStats()
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val fechaSql = DateFormatter.todayDbFormat()
            val fechaBici = DateFormatter.todayDisplayFormat()

            pointStats = listOf(
                ReportManager.SummaryPointRow(
                    ControlPoint.PORTERIA_2.id,
                    getString(R.string.porteria_2),
                    registroRepository.getCountRegistrosByFechaAndPoint(fechaSql, ControlPoint.PORTERIA_2.id),
                    registroRepository.getCountSalidasByFechaAndPoint(fechaSql, ControlPoint.PORTERIA_2.id)
                ),
                ReportManager.SummaryPointRow(
                    ControlPoint.PORTERIA_3.id,
                    getString(R.string.porteria_3),
                    registroRepository.getCountRegistrosByFechaAndPoint(fechaSql, ControlPoint.PORTERIA_3.id),
                    registroRepository.getCountSalidasByFechaAndPoint(fechaSql, ControlPoint.PORTERIA_3.id)
                ),
                ReportManager.SummaryPointRow(
                    ControlPoint.PORTERIA_4.id,
                    getString(R.string.porteria_4),
                    registroRepository.getCountRegistrosByFechaAndPoint(fechaSql, ControlPoint.PORTERIA_4.id),
                    registroRepository.getCountSalidasByFechaAndPoint(fechaSql, ControlPoint.PORTERIA_4.id)
                ),
                ReportManager.SummaryPointRow(
                    ControlPoint.BICICLETERO_1.id,
                    getString(R.string.bicicletero_1),
                    bicicletaRepository.countByFechaAndPoint(fechaBici, ControlPoint.BICICLETERO_1.id),
                    bicicletaRepository.countSalidasByFechaAndPoint(fechaBici, ControlPoint.BICICLETERO_1.id)
                ),
                ReportManager.SummaryPointRow(
                    ControlPoint.BICICLETERO_2.id,
                    getString(R.string.bicicletero_2),
                    bicicletaRepository.countByFechaAndPoint(fechaBici, ControlPoint.BICICLETERO_2.id),
                    bicicletaRepository.countSalidasByFechaAndPoint(fechaBici, ControlPoint.BICICLETERO_2.id)
                )
            )

            renderPointStats(pointStats)

            val selected = when (selectedScope) {
                ScopeOption.GENERAL -> ReportManager.SummaryPointRow("general", "General", pointStats.sumOf { it.entradas }, pointStats.sumOf { it.salidas })
                else -> pointStats.first { it.label == getString(selectedScope.labelRes) }
            }

            binding.tvEntraronCount.text = selected.entradas.toString()
            binding.tvSalieronCount.text = selected.salidas.toString()
            binding.tvDentroCount.text = selected.dentro.toString()
        }
    }

    private fun renderPointStats(stats: List<ReportManager.SummaryPointRow>) {
        binding.llPointStats.removeAllViews()
        stats.forEach { stat ->
            binding.llPointStats.addView(createPointCard(stat))
        }
    }

    private fun createPointCard(stat: ReportManager.SummaryPointRow): android.view.View {
        val binding = ItemPointStatBinding.inflate(layoutInflater, this.binding.llPointStats, false)

        binding.tvPointName.text = stat.label
        binding.tvPointStats.text = "E:${stat.entradas}  S:${stat.salidas}  D:${stat.dentro}"

        val color = if (stat.dentro > 0) R.color.bicicletero2_primary else R.color.text_hint
        binding.tvPointStats.setTextColor(ContextCompat.getColor(this, color))

        return binding.root
    }

    private fun setupListeners() {
        binding.cardReports.setOnClickListener {
            startActivity(Intent(this, ShiftReportActivity::class.java))
        }

        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, com.dur1al.controlate.porteria.ui.settings.SettingsActivity::class.java))
        }

        binding.btnExportAdminSummary.setOnClickListener {
            lifecycleScope.launch {
                val fechaSql = DateFormatter.todayDbFormat()
                val fechaBici = DateFormatter.todayDisplayFormat()
                val registrosPorteria: List<RegistroIngreso> = registroRepository.getRecordsByDate(fechaSql)
                val registrosBici: List<RegistroBicicleta> = bicicletaRepository.getRecordsByDate(fechaBici)
                when (val result = reportManager.generateAdminSummaryExcel(pointStats, registrosPorteria, registrosBici, fechaSql, fechaBici)) {
                    is ExportResult.Success -> Snackbar.make(binding.root, getString(R.string.admin_export_success, result.file.name), Snackbar.LENGTH_LONG).show()
                    is ExportResult.EmptyData -> Snackbar.make(binding.root, R.string.admin_export_empty, Snackbar.LENGTH_SHORT).show()
                    is ExportResult.Error -> Snackbar.make(binding.root, getString(R.string.admin_export_error, result.message), Snackbar.LENGTH_LONG).show()
                }
            }
        }

        binding.btnLimpiarTodo.setOnClickListener {
            showConfirmDialog(
                getString(R.string.admin_delete_operational_title),
                getString(R.string.admin_delete_operational_message)
            ) {
                viewModel.clearAllData()
                Snackbar.make(binding.root, R.string.admin_delete_operational_success, Snackbar.LENGTH_SHORT).show()
                loadStats()
            }
        }
    }

    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_confirm) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
