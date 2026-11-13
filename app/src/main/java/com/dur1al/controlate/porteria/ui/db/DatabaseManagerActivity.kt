package com.dur1al.controlate.porteria.ui.db

import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityDbManagerBinding
import com.dur1al.controlate.databinding.DialogImportMassiveBinding
import com.dur1al.controlate.porteria.domain.importer.PersonaCsvParser
import com.dur1al.controlate.porteria.ui.common.collectWhenStarted
import com.dur1al.controlate.porteria.ui.common.shareReport
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Gestión de datos del directorio: ver/editar, importación masiva y exportación PDF. */
class DatabaseManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDbManagerBinding
    private val viewModel: DirectoryViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDbManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.cardViewDirectory.setOnClickListener { startActivity(Intent(this, AdminDirectoryActivity::class.java)) }
        binding.cardImport.setOnClickListener { showImportDialog() }
        binding.cardExport.setOnClickListener { viewModel.exportDirectory() }

        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is DirectoryViewModel.Event.Share ->
                    if (!shareReport(event.file)) {
                        Snackbar.make(binding.root, R.string.share_report_unavailable, Snackbar.LENGTH_LONG).show()
                    }
                is DirectoryViewModel.Event.ImportFailed -> showImportErrors(event.errors)
                is DirectoryViewModel.Event.Message ->
                    Snackbar.make(binding.root, event.message.resolve(this), Snackbar.LENGTH_LONG).show()
                DirectoryViewModel.Event.EditSaved -> Unit // La edición vive en AdminDirectoryActivity.
            }
        }
    }

    private fun showImportDialog() {
        val dialog = DialogImportMassiveBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.database_mass_import)
            .setMessage(R.string.import_format)
            .setView(dialog.root)
            .setPositiveButton(R.string.import_button) { _, _ -> viewModel.import(dialog.etImportContent.text.toString()) }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showImportErrors(errors: List<PersonaCsvParser.LineError>) {
        val preview = errors.take(MAX_ERRORS_SHOWN).joinToString("\n") { getString(it.reason.messageRes(), it.line) }
        val extra = errors.size - MAX_ERRORS_SHOWN
        val message = if (extra > 0) "$preview\n${getString(R.string.db_import_more_errors, extra)}" else preview
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.db_import_review)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_close, null)
            .show()
    }

    @StringRes
    private fun PersonaCsvParser.LineError.Reason.messageRes() = when (this) {
        PersonaCsvParser.LineError.Reason.MISSING_FIELDS -> R.string.db_import_error_min_fields
        PersonaCsvParser.LineError.Reason.INVALID_PLATE -> R.string.db_import_error_plate
        PersonaCsvParser.LineError.Reason.MISSING_NAME -> R.string.db_import_error_name
        PersonaCsvParser.LineError.Reason.INVALID_RUT -> R.string.db_import_error_rut
    }

    private companion object {
        const val MAX_ERRORS_SHOWN = 6
    }
}
