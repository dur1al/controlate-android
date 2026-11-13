package com.dur1al.controlate.porteria.ui.directory

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.dur1al.controlate.App
import com.dur1al.controlate.databinding.ActivityDirectoryBinding
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.ui.dashboard.DashboardViewModel
import com.dur1al.controlate.porteria.ui.db.PersonaAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

class DirectoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDirectoryBinding
    private val viewModel: DashboardViewModel by viewModel()
    private lateinit var adapter: PersonaAdapter

    private var patenteQuery = ""
    private var nombreQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDirectoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeDirectory()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = PersonaAdapter(isReadOnly = true)
        binding.rvDirectory.apply {
            layoutManager = LinearLayoutManager(this@DirectoryActivity)
            adapter = this@DirectoryActivity.adapter
        }
    }

    private fun observeDirectory() {
        viewModel.allPersonas.observe(this) { lista ->
            filterList(lista ?: emptyList())
        }
    }

    private fun setupListeners() {
        binding.etSearchPatente.doOnTextChanged { text, _, _, _ ->
            patenteQuery = text.toString().uppercase()
            viewModel.allPersonas.value?.let { filterList(it) }
        }

        binding.etSearchNombre.doOnTextChanged { text, _, _, _ ->
            nombreQuery = text.toString().lowercase()
            viewModel.allPersonas.value?.let { filterList(it) }
        }

        binding.fabAddPersona.visibility = View.GONE
    }

    private fun filterList(lista: List<Persona>) {
        val filtered = lista.filter {
            it.patente.contains(patenteQuery) &&
            it.nombreApellido.lowercase().contains(nombreQuery)
        }
        adapter.submitList(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}
