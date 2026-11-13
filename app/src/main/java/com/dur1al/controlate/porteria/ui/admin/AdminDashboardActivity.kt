package com.dur1al.controlate.porteria.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.dur1al.controlate.App
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityAdminDashboardBinding
import com.dur1al.controlate.porteria.utils.ExportResult
import org.koin.androidx.viewmodel.ext.android.viewModel

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var logAdapter: AdminLogAdapter
    
    private val viewModel: AdminViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        logAdapter = AdminLogAdapter()
        binding.rvLiveLogs.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@AdminDashboardActivity)
            adapter = logAdapter
        }

        viewModel.stats.observe(this) { stats ->
            binding.tvIngresos.text = stats.ingresos.toString()
            binding.tvSalidas.text = stats.salidas.toString()
            binding.tvEnRecinto.text = stats.enRecinto.toString()
        }

        viewModel.recentLogs.observe(this) { logs ->
            logAdapter.submitList(logs)
        }

        viewModel.exportResult.observe(this) { result ->
            if (result == null) return@observe
            
            binding.progressBar.isVisible = false
            when (result) {
                is ExportResult.Success -> Toast.makeText(this@AdminDashboardActivity, R.string.admin_dashboard_export_success, Toast.LENGTH_SHORT).show()
                is ExportResult.EmptyData -> Toast.makeText(this@AdminDashboardActivity, R.string.admin_dashboard_export_empty, Toast.LENGTH_SHORT).show()
                is ExportResult.Error -> Toast.makeText(this@AdminDashboardActivity, getString(R.string.admin_dashboard_export_error, result.message), Toast.LENGTH_LONG).show()
            }
            viewModel.clearExportResult()
        }

        viewModel.loadStats()

        binding.btnExport.setOnClickListener {
            binding.progressBar.isVisible = true
            viewModel.exportReport()
        }
    }
}
