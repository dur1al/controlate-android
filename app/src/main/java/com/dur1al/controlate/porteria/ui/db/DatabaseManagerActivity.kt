package com.dur1al.controlate.porteria.ui.db

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.dur1al.controlate.App
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityDbManagerBinding
import com.dur1al.controlate.databinding.DialogImportMassiveBinding
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.session.SessionManager
import com.dur1al.controlate.porteria.ui.dashboard.DashboardViewModel
import com.dur1al.controlate.porteria.utils.ExportResult
import com.dur1al.controlate.porteria.utils.ReportManager
import com.dur1al.controlate.porteria.utils.RutUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class DatabaseManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDbManagerBinding
    private val viewModel: DashboardViewModel by viewModel()
    private val personaRepository: com.dur1al.controlate.porteria.data.repository.PersonaRepository by inject()
    private lateinit var reportManager: ReportManager
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDbManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reportManager = ReportManager(this)
        sessionManager = SessionManager(this)

        setupToolbar()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        binding.cardViewDirectory.setOnClickListener {
            startActivity(Intent(this, AdminDirectoryActivity::class.java))
        }

        binding.cardImport.setOnClickListener { showImportDialog() }

        binding.cardExport.setOnClickListener {
            lifecycleScope.launch {
                val personas = personaRepository.allPersonas.first()
                if (personas.isNotEmpty()) {
                    when (val result = reportManager.exportDatabaseExcel(personas)) {
                        is ExportResult.Success -> Snackbar.make(binding.root, getString(R.string.db_export_success, result.file.name), Snackbar.LENGTH_LONG).show()
                        is ExportResult.EmptyData -> Snackbar.make(binding.root, R.string.db_export_empty, Snackbar.LENGTH_SHORT).show()
                        is ExportResult.Error -> Snackbar.make(binding.root, getString(R.string.db_export_error, result.message), Snackbar.LENGTH_LONG).show()
                    }
                } else {
                    Snackbar.make(binding.root, R.string.db_no_directory_data, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showImportDialog() {
        val dialogBinding = DialogImportMassiveBinding.inflate(LayoutInflater.from(this))

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.database_mass_import)
            .setMessage(R.string.import_format)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.import_button) { _, _ ->
                val content = dialogBinding.etImportContent.text.toString()
                if (content.isNotBlank()) {
                    processImport(content)
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun processImport(content: String) {
        val lines = content.lines().filter { it.isNotBlank() }
        val personas = mutableListOf<Persona>()
        val errors = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            val parts = line.split(",").map { it.trim() }
            if (parts.size < 2) {
                errors.add(getString(R.string.db_import_error_min_fields, lineNumber))
                return@forEachIndexed
            }

            val patente = parts.getOrNull(0).orEmpty().uppercase().replace(" ", "")
            val nombre = parts.getOrNull(1).orEmpty()
            val rut = parts.getOrNull(3).orEmpty().let(RutUtils::cleanRut)

            if (patente.length < 4) {
                errors.add(getString(R.string.db_import_error_plate, lineNumber))
                return@forEachIndexed
            }
            if (nombre.isBlank()) {
                errors.add(getString(R.string.db_import_error_name, lineNumber))
                return@forEachIndexed
            }
            if (rut.isNotBlank() && !RutUtils.isValidRut(rut)) {
                errors.add(getString(R.string.db_import_error_rut, lineNumber))
                return@forEachIndexed
            }

            personas.add(
                Persona(
                    patente = patente,
                    nombreApellido = nombre,
                    departamento = parts.getOrNull(2)?.ifBlank { "-" } ?: "-",
                    rut = rut.ifBlank { null },
                    padron = parts.getOrNull(4)?.ifBlank { null },
                    empresa = parts.getOrNull(5)?.ifBlank { null },
                    descripcionAuto = parts.getOrNull(6)?.ifBlank { "-" } ?: "-",
                    tipoPersona = parts.getOrNull(7)?.ifBlank { "Funcionario" } ?: "Funcionario"
                )
            )
        }

        if (errors.isNotEmpty()) {
            val preview = errors.take(6).joinToString("\n")
            val suffix = if (errors.size > 6) "\n" + getString(R.string.db_import_more_errors, errors.size - 6) else ""
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.db_import_review)
                .setMessage(preview + suffix)
                .setPositiveButton(R.string.dialog_close, null)
                .show()
            return
        }

        if (personas.isNotEmpty()) {
            viewModel.importPersonas(personas)
            Snackbar.make(binding.root, getString(R.string.db_import_success, personas.size), Snackbar.LENGTH_LONG).show()
        }
    }
}
