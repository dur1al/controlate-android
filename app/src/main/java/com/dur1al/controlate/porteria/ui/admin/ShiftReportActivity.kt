package com.dur1al.controlate.porteria.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.dur1al.controlate.App
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityShiftReportBinding
import com.dur1al.controlate.databinding.ItemShiftBinding
import com.dur1al.controlate.porteria.data.local.dao.BicicletaDao
import com.dur1al.controlate.porteria.data.local.dao.RegistroDao
import com.dur1al.controlate.porteria.domain.util.DateFormatter
import com.dur1al.controlate.porteria.ui.admin.reports.ReportShiftAdapter
import com.dur1al.controlate.porteria.ui.admin.reports.ReportShiftUi
import com.dur1al.controlate.porteria.ui.admin.reports.ReportType
import com.dur1al.controlate.porteria.utils.ExportResult
import com.dur1al.controlate.porteria.utils.ReportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.koin.android.ext.android.inject

class ShiftReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShiftReportBinding
    private lateinit var reportManager: ReportManager

    private val registroRepository: com.dur1al.controlate.porteria.data.repository.RegistroRepository by inject()
    private val bicicletaRepository: com.dur1al.controlate.porteria.data.repository.BicicletaRepository by inject()

    private var allShiftItems: List<ReportShiftUi> = emptyList()
    private var activeDateFilterKey: String? = null
    private val subtitleBase by lazy { getString(R.string.reports_subtitle) }

    private val adapter = ReportShiftAdapter(
        onExportClick = { shift -> exportShift(shift) },
        onDeleteClick = { shift -> confirmDeleteShift(shift) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShiftReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reportManager = ReportManager(this)
        setupToolbar()
        setupRecyclerView()
        observeShifts()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_SAVED_REPORTS -> {
                    showSavedReportsDialog()
                    true
                }
                MENU_FILTER_DATE -> {
                    showShiftDateFilterDialog()
                    true
                }
                MENU_CLEAR_FILTER -> {
                    clearDateFilter()
                    true
                }
                else -> false
            }
        }
        binding.toolbar.menu.add(0, MENU_FILTER_DATE, 0, getString(R.string.reports_menu_filter_date))
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER)
        binding.toolbar.menu.add(0, MENU_CLEAR_FILTER, 1, getString(R.string.reports_menu_clear_filter))
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER)
        binding.toolbar.menu.add(0, MENU_SAVED_REPORTS, 2, getString(R.string.reports_menu_saved))
            .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    private fun setupRecyclerView() {
        binding.rvShifts.layoutManager = LinearLayoutManager(this)
        binding.rvShifts.adapter = adapter
    }

    private fun observeShifts() {
        lifecycleScope.launch {
            combine(registroRepository.allShifts, bicicletaRepository.allShiftSummaries) { porteriaShifts, bicicletaShifts ->
                buildUnifiedList(porteriaShifts, bicicletaShifts)
            }.collect { shifts ->
                allShiftItems = shifts
                applyShiftFilter()
            }
        }
    }

    private fun buildUnifiedList(
        porteriaShifts: List<RegistroDao.ShiftSummary>,
        bicicletaShifts: List<BicicletaDao.BicicletaShiftSummary>
    ): List<ReportShiftUi> {
        val porteriaItems = porteriaShifts.map { shift ->
            ReportShiftUi(
                id = "${ReportType.PORTERIA}-${shift.shiftId}",
                shiftId = shift.shiftId,
                type = ReportType.PORTERIA,
                title = shift.guardiaTurno.ifBlank { "Guardia" },
                point = shift.porteria,
                date = shift.fechaTurno,
                shiftLabel = shift.turnoTipo,
                startTime = shift.horaInicio,
                endTime = shift.horaFin,
                totalRecords = shift.totalRegistros,
                totalClosed = shift.totalSalidas
            )
        }

        val bicicletaItems = bicicletaShifts.map { shift ->
            val point = formatBikePoint(shift.controlPoint)
            ReportShiftUi(
                id = "${ReportType.BICICLETERO}-${shift.shiftId}",
                shiftId = shift.shiftId,
                type = ReportType.BICICLETERO,
                title = shift.guardiaTurno,
                point = point,
                date = shift.fecha,
                shiftLabel = shift.turnoTipo,
                startTime = shift.horaInicio,
                endTime = shift.horaFin,
                totalRecords = shift.totalRegistros,
                totalClosed = shift.totalSalidas
            )
        }

        return (porteriaItems + bicicletaItems).sortedWith(
            compareByDescending<ReportShiftUi> { parseDateKey(it.date) ?: "0000-00-00" }
                .thenByDescending { it.shiftId }
        )
    }

    private fun applyShiftFilter() {
        val filtered = activeDateFilterKey?.let { key ->
            allShiftItems.filter { parseDateKey(it.date) == key }
        } ?: allShiftItems

        adapter.submitList(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.tvSubtitle.text = activeDateFilterKey?.let { key ->
            "$subtitleBase\n${getString(R.string.reports_filter_subtitle, formatDateKeyForDisplay(key))}"
        } ?: subtitleBase
    }

    private fun showShiftDateFilterDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                activeDateFilterKey = KEY_DATE_FORMAT.format(selectedCalendar.time)
                applyShiftFilter()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun clearDateFilter() {
        activeDateFilterKey = null
        applyShiftFilter()
    }

    private fun exportShift(shift: ReportShiftUi) {
        lifecycleScope.launch {
            val result = when (shift.type) {
                ReportType.PORTERIA -> {
                    val records = registroRepository.getRecordsByShiftList(shift.shiftId)
                    reportManager.generateAndShareExcel(
                        registros = records,
                        guardiaName = shift.title,
                        porteriaName = shift.point,
                        fechaTurno = shift.date,
                        turnoTipo = shift.shiftLabel
                    )
                }
                ReportType.BICICLETERO -> {
                    val records = bicicletaRepository.getRecordsByShiftList(shift.shiftId)
                    reportManager.generateBicicletaReportExcel(
                        registros = records,
                        guardiaName = shift.title,
                        turnoTipo = shift.shiftLabel,
                        shareAfterSave = true
                    )
                }
            }

            when (result) {
                is ExportResult.Success -> {
                    Snackbar.make(binding.root, getString(R.string.reports_export_success, result.file.name), Snackbar.LENGTH_LONG).show()
                }
                is ExportResult.EmptyData -> {
                    showEmptyExportMessage()
                }
                is ExportResult.Error -> {
                    Snackbar.make(binding.root, getString(R.string.reports_export_error, result.message), Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmDeleteShift(shift: ReportShiftUi) {
        AlertDialog.Builder(this)
            .setTitle(R.string.reports_delete_title)
            .setMessage(R.string.reports_delete_message)
            .setPositiveButton(R.string.reports_delete_confirm) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    if (shift.type == ReportType.PORTERIA) {
                        registroRepository.deletePorteriaShift(shift.shiftId)
                    } else {
                        bicicletaRepository.deleteByShift(shift.shiftId)
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showSavedReportsDialog() {
        val files = reportManager.listSavedReports()
        if (files.isEmpty()) {
            Snackbar.make(binding.root, R.string.reports_saved_empty, Snackbar.LENGTH_SHORT).show()
            return
        }

        val savedItems = files.map { file ->
            val modified = Date(file.lastModified())
            val dayKey = KEY_DATE_FORMAT.format(modified)
            val timeLabel = TIME_LABEL_FORMAT.format(modified)
            SavedReportItem(
                file = file,
                dayKey = dayKey,
                displayLabel = getString(R.string.reports_saved_item, timeLabel, file.name)
            )
        }

        val groupedByDay = savedItems.groupBy { it.dayKey }
        val orderedDayKeys = groupedByDay.keys.sortedDescending()
        val dayOptions = mutableListOf(getString(R.string.reports_saved_all_count, savedItems.size)).apply {
            addAll(orderedDayKeys.map { key ->
                val count = groupedByDay[key]?.size ?: 0
                getString(R.string.reports_saved_day_count, formatDateKeyForDisplay(key), count)
            })
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.reports_saved_title)
            .setItems(dayOptions.toTypedArray()) { _, which ->
                val filtered = if (which == 0) {
                    savedItems
                } else {
                    val selectedKey = orderedDayKeys[which - 1]
                    groupedByDay[selectedKey].orEmpty()
                }
                showSavedReportsList(filtered)
            }
            .setNegativeButton(R.string.dialog_close, null)
            .show()
    }

    private fun showSavedReportsList(items: List<SavedReportItem>) {
        if (items.isEmpty()) {
            Snackbar.make(binding.root, R.string.reports_saved_empty_day, Snackbar.LENGTH_SHORT).show()
            return
        }

        val labels = items.map { it.displayLabel }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.reports_saved_pick)
            .setItems(labels) { _, which ->
                reportManager.shareExistingReport(items[which].file)
            }
            .setNegativeButton(R.string.dialog_close, null)
            .show()
    }

    private fun showEmptyExportMessage() {
        Snackbar.make(binding.root, R.string.reports_export_empty, Snackbar.LENGTH_SHORT).show()
    }

    private fun formatBikePoint(value: String): String {
        return when (value) {
            "BICICLETERO_2" -> getString(R.string.bicicletero_2)
            else -> getString(R.string.bicicletero_1)
        }
    }

    private fun parseDateKey(value: String): String? {
        val date = parseDate(value) ?: return null
        return KEY_DATE_FORMAT.format(date)
    }

    private fun parseDate(value: String): Date? {
        return DateFormatter.parseFlexible(value)
    }

    private fun formatDateKeyForDisplay(key: String): String {
        val parsed = KEY_DATE_FORMAT.parse(key) ?: return key
        return DAY_LABEL_FORMAT.format(parsed)
    }

    private data class SavedReportItem(
        val file: File,
        val dayKey: String,
        val displayLabel: String
    )

    companion object {
        private const val MENU_SAVED_REPORTS = 1001
        private const val MENU_FILTER_DATE = 1002
        private const val MENU_CLEAR_FILTER = 1003
        private val KEY_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val DAY_LABEL_FORMAT = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        private val TIME_LABEL_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
    }
}
