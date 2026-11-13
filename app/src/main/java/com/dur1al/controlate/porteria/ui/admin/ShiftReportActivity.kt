package com.dur1al.controlate.porteria.ui.admin

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityShiftReportBinding
import com.dur1al.controlate.porteria.domain.model.ShiftSummary
import com.dur1al.controlate.porteria.domain.time.TimeFormat
import com.dur1al.controlate.porteria.ui.common.collectWhenStarted
import com.dur1al.controlate.porteria.ui.common.shareReport
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

/** Turnos registrados (porterías y bicicleteros) con exportación PDF y borrado. */
class ShiftReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShiftReportBinding
    private val viewModel: ShiftReportsViewModel by viewModel()

    private val adapter = ShiftSummaryAdapter(
        onExport = { viewModel.export(it.shiftId) },
        onDelete = ::confirmDelete
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShiftReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvShifts.layoutManager = LinearLayoutManager(this)
        binding.rvShifts.adapter = adapter
        setUpToolbar()

        collectWhenStarted(viewModel.state) { state ->
            adapter.submitList(state.shifts)
            binding.tvEmpty.isVisible = !state.loading && state.shifts.isEmpty()
            val base = getString(R.string.reports_subtitle)
            binding.tvSubtitle.text = state.dateFilter?.let {
                "$base\n${getString(R.string.reports_filter_subtitle, TimeFormat.isoToDisplay(it))}"
            } ?: base
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is ShiftReportsViewModel.Event.Share ->
                    if (!shareReport(event.file)) {
                        Snackbar.make(binding.root, R.string.share_report_unavailable, Snackbar.LENGTH_LONG).show()
                    }
                is ShiftReportsViewModel.Event.SavedReports -> showSavedReports(event.files)
                is ShiftReportsViewModel.Event.Message ->
                    Snackbar.make(binding.root, event.message.resolve(this), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setUpToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.menu.add(0, MENU_FILTER_DATE, 0, R.string.reports_menu_filter_date)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        binding.toolbar.menu.add(0, MENU_CLEAR_FILTER, 1, R.string.reports_menu_clear_filter)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        binding.toolbar.menu.add(0, MENU_SAVED, 2, R.string.reports_menu_saved)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_FILTER_DATE -> pickDate()
                MENU_CLEAR_FILTER -> viewModel.filterByDate(null)
                MENU_SAVED -> viewModel.loadSavedReports()
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
    }

    private fun pickDate() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.reports_menu_filter_date)
            .build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            // MaterialDatePicker entrega medianoche UTC del día elegido.
            viewModel.filterByDate(Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate().toString())
        }
        picker.show(supportFragmentManager, "date_filter")
    }

    private fun confirmDelete(shift: ShiftSummary) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reports_delete_title)
            .setMessage(R.string.reports_delete_message)
            .setPositiveButton(R.string.reports_delete_confirm) { _, _ -> viewModel.delete(shift.shiftId) }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showSavedReports(files: List<File>) {
        val labels = files.map {
            getString(R.string.reports_saved_item, TimeFormat.displayDateTime(it.lastModified()), it.name)
        }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reports_saved_pick)
            .setItems(labels) { _, which -> shareReport(files[which]) }
            .setNegativeButton(R.string.dialog_close, null)
            .show()
    }

    private companion object {
        const val MENU_FILTER_DATE = 1
        const val MENU_CLEAR_FILTER = 2
        const val MENU_SAVED = 3
    }
}
