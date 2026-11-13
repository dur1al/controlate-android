package com.dur1al.controlate

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.dur1al.controlate.databinding.ActivityMainBinding
import com.dur1al.controlate.porteria.data.ControlPoint
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.data.local.session.SessionManager
import com.dur1al.controlate.porteria.ui.admin.AdminActivity
import com.dur1al.controlate.porteria.ui.dashboard.DashboardViewModel
import com.dur1al.controlate.porteria.ui.dashboard.EntradaBottomSheet
import com.dur1al.controlate.porteria.ui.dashboard.RegistroAdapter
import com.dur1al.controlate.porteria.ui.login.LoginActivity
import com.dur1al.controlate.porteria.utils.ReportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentPoint: ControlPoint = ControlPoint.PORTERIA_2

    private val viewModel: DashboardViewModel by viewModel()

    private lateinit var adapter: RegistroAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentPoint = resolveControlPoint(intent.getStringExtra(EXTRA_CONTROL_POINT_NAME))
        applyThemeForPoint(currentPoint)

        sessionManager = SessionManager(this)
        setupRecyclerView()
        setupObservers()
        setupListeners()
        viewModel.setShiftContext(sessionManager.getShiftId())
    }

    private fun resolveControlPoint(rawName: String?): ControlPoint {
        if (rawName.isNullOrBlank()) return ControlPoint.PORTERIA_2
        return runCatching { ControlPoint.valueOf(rawName) }
            .getOrDefault(ControlPoint.PORTERIA_2)
    }

    private fun applyThemeForPoint(point: ControlPoint) {
        val colorInt = ContextCompat.getColor(this, point.primaryColorRes)
        binding.fabNuevoRedesign.backgroundTintList = ColorStateList.valueOf(colorInt)
        binding.headerLayout.setBackgroundColor(colorInt)
    }

    private fun setupRecyclerView() {
        adapter = RegistroAdapter(
            onExitClick = { viewModel.markExit(it) },
            onItemClick = { registro ->
                viewModel.setRegistroToEdit(registro)
                EntradaBottomSheet().show(supportFragmentManager, EntradaBottomSheet.TAG)
            },
            onLongClick = { showOptionsDialog(it) }
        )
        binding.rvActivos.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupObservers() {
        viewModel.registrosToShow.observe(this) { registros ->
            adapter.submitList(registros)
            binding.emptyStateLayout.isVisible = registros.isEmpty()
            binding.rvActivos.isVisible = registros.isNotEmpty()
        }
    }

    private fun setupListeners() {
        binding.fabNuevoRedesign.setOnClickListener {
            viewModel.setRegistroToEdit(null)
            EntradaBottomSheet().show(supportFragmentManager, EntradaBottomSheet.TAG)
        }

        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            viewModel.updateSearchQuery(text.toString())
        }

        binding.btnEndShift.setOnClickListener { confirmEndShift() }
    }

    private fun confirmEndShift() {
        AlertDialog.Builder(this)
            .setTitle(R.string.end_shift_title)
            .setMessage(R.string.end_shift_message)
            .setPositiveButton(R.string.dialog_yes) { _, _ -> endShiftAndReturnToLogin() }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun endShiftAndReturnToLogin() {
        sessionManager.endShift()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showOptionsDialog(registro: RegistroIngreso) {
        Snackbar.make(binding.root, getString(R.string.admin_only_edit_records), Snackbar.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_CONTROL_POINT_NAME = "CONTROL_POINT_NAME"
    }
}

