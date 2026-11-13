package com.dur1al.controlate.porteria.ui.db

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.dur1al.controlate.App
import com.dur1al.controlate.databinding.ActivityAdminDirectoryBinding
import com.dur1al.controlate.databinding.BottomSheetEntradaBinding
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.ui.dashboard.DashboardViewModel
import com.dur1al.controlate.porteria.utils.RutUtils
import com.dur1al.controlate.porteria.utils.applyRutFormatter
import org.koin.androidx.viewmodel.ext.android.viewModel

class AdminDirectoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDirectoryBinding
    private val viewModel: DashboardViewModel by viewModel()
    private lateinit var adapter: PersonaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDirectoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = PersonaAdapter(
            onEdit = { persona -> showEditPersonaDialog(persona) },
            onDelete = { persona -> showDeleteConfirmDialog(persona) }
        )
        binding.rvDirectory.layoutManager = LinearLayoutManager(this)
        binding.rvDirectory.adapter = adapter
    }

    private fun setupListeners() {
        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            viewModel.updateDbSearchQuery(text.toString())
        }

        binding.fabImport.visibility = View.GONE // Movido a DatabaseManager
    }

    private fun observeData() {
        viewModel.filteredPersonas.observe(this) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showEditPersonaDialog(persona: Persona) {
        val dialogBinding = BottomSheetEntradaBinding.inflate(LayoutInflater.from(this))

        // Rellenar datos actuales
        dialogBinding.apply {
            etRut.applyRutFormatter()
            etPatente.setText(persona.patente)
            etNombre.setText(persona.nombreApellido)
            etDepto.setText(persona.departamento)
            etEmpresa.setText(persona.empresa ?: "")
            etRut.setText(persona.rut ?: "")
            etPadron.setText(persona.padron ?: "")
            etAuto.setText(persona.descripcionAuto)

            when(persona.tipoPersona) {
                "Visita" -> rbVisita.isChecked = true
                "Estudiante" -> rbEstudiante.isChecked = true
                "Funcionario" -> rbFuncionario.isChecked = true
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Registro")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val updatedPersona = persona.copy(
                    patente = dialogBinding.etPatente.text.toString().uppercase(),
                    nombreApellido = dialogBinding.etNombre.text.toString(),
                    departamento = dialogBinding.etDepto.text.toString(),
                    empresa = dialogBinding.etEmpresa.text.toString(),
                    rut = RutUtils.cleanRut(dialogBinding.etRut.text.toString()).ifBlank { null },
                    padron = dialogBinding.etPadron.text.toString(),
                    descripcionAuto = dialogBinding.etAuto.text.toString(),
                    tipoPersona = when(dialogBinding.rgTipoPersona.checkedRadioButtonId) {
                        dialogBinding.rbEstudiante.id -> "Estudiante"
                        dialogBinding.rbFuncionario.id -> "Funcionario"
                        else -> "Visita"
                    }
                )
                viewModel.updatePersona(updatedPersona)
                Snackbar.make(binding.root, "Registro actualizado", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmDialog(persona: Persona) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Registro")
            .setMessage("¿Estás seguro de eliminar a ${persona.nombreApellido}?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deletePersona(persona)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
