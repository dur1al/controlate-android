package com.dur1al.controlate.porteria.ui.bicicletero

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.dur1al.controlate.App
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityBicicleteroDashboardBinding
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.session.SessionManager
import com.dur1al.controlate.porteria.ui.login.LoginActivity
import com.dur1al.controlate.porteria.utils.ExportResult
import com.dur1al.controlate.porteria.utils.ReportManager
import com.dur1al.controlate.porteria.utils.RutUtils
import com.dur1al.controlate.porteria.utils.applyRutFormatter
import com.dur1al.controlate.porteria.domain.util.DateFormatter
import kotlinx.coroutines.launch
import android.view.Menu
import android.view.MenuItem

import org.koin.androidx.viewmodel.ext.android.viewModel

class BicicleteroDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBicicleteroDashboardBinding
    private val viewModel: BicicleteroViewModel by viewModel()
    private lateinit var reportManager: ReportManager
    private lateinit var sessionManager: SessionManager
    private var currentShiftRegistros: List<RegistroBicicleta> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBicicleteroDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reportManager = ReportManager(this)
        sessionManager = SessionManager(this)
        applyGreenThemeByPoint()
        setSupportActionBar(binding.toolbar)

        binding.etSearch.applyRutFormatter()

        val adapter = BicicletaAdapter { registro ->
            if (registro.horaSalida == null) {
                val horaSalida = DateFormatter.nowTime()
                viewModel.actualizarRegistro(registro.copy(horaSalida = horaSalida))
                
                Snackbar.make(binding.root, R.string.exit_registered, Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.rvBicicletas.layoutManager = LinearLayoutManager(this)
        binding.rvBicicletas.adapter = adapter

        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            val query = RutUtils.cleanRut(text.toString())
            val filtrada = if (query.isEmpty()) currentShiftRegistros else currentShiftRegistros.filter { it.rut.contains(query, ignoreCase = true) }
            adapter.submitList(filtrada)
        }

        viewModel.getRegistrosByShift(sessionManager.getShiftId()).observe(this) { registros ->
            currentShiftRegistros = registros
            val query = RutUtils.cleanRut(binding.etSearch.text?.toString().orEmpty())
            val filtrada = if (query.isEmpty()) registros else registros.filter { it.rut.contains(query, ignoreCase = true) }
            adapter.submitList(filtrada)
            binding.emptyStateLayout.isVisible = filtrada.isEmpty()
            binding.rvBicicletas.isVisible = filtrada.isNotEmpty()
        }

        binding.btnNuevo.setOnClickListener {
            BicicletaBottomSheet().show(supportFragmentManager, BicicletaBottomSheet.TAG)
        }

        binding.btnEndShift.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.end_shift_title)
                .setMessage(R.string.end_shift_bike_message)
                .setPositiveButton(R.string.dialog_yes_finish) { _, _ -> endShiftAndReturnToLogin() }
                .setNegativeButton(R.string.dialog_no, null)
                .show()
        }
    }

    private fun applyGreenThemeByPoint() {
        val isBici2 = sessionManager.getPorteriaName().contains("Bicicletero 2", ignoreCase = true)
        val primaryColor = ContextCompat.getColor(this, if (isBici2) R.color.bicicletero2_primary else R.color.bicicletero2_variant)
        binding.headerLayout.setBackgroundColor(primaryColor)
        binding.toolbar.setBackgroundColor(primaryColor)
        binding.btnNuevo.backgroundTintList = ColorStateList.valueOf(primaryColor)
    }

    private fun endShiftAndReturnToLogin() {
        lifecycleScope.launch {
            Log.d("Bicicletero", "Exportando registros finales: ${currentShiftRegistros.size}")
            val result = reportManager.generateBicicletaReportExcel(
                registros = currentShiftRegistros,
                guardiaName = sessionManager.getGuardiaName().ifBlank { "No registrado" },
                turnoTipo = sessionManager.getTurnoTipo(),
                shareAfterSave = false
            )
            
            if (result is ExportResult.Error) {
                Log.e("Bicicletero", "Error al exportar reporte final: ${result.message}")
            }

            sessionManager.endShift()
            val intent = Intent(this@BicicleteroDashboardActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bicicletero, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_exportar) {
            lifecycleScope.launch {
                val result = reportManager.generateBicicletaReportExcel(
                    registros = currentShiftRegistros,
                    guardiaName = sessionManager.getGuardiaName().ifBlank { "No registrado" },
                    turnoTipo = sessionManager.getTurnoTipo(),
                    shareAfterSave = true
                )
                when(result) {
                    is ExportResult.Success -> Snackbar.make(binding.root, R.string.export_generated_saved, Snackbar.LENGTH_SHORT).show()
                    is ExportResult.Error -> Snackbar.make(binding.root, getString(R.string.bici_export_error, result.message), Snackbar.LENGTH_LONG).show()
                    is ExportResult.EmptyData -> Snackbar.make(binding.root, R.string.bici_export_empty, Snackbar.LENGTH_SHORT).show()
                }
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

