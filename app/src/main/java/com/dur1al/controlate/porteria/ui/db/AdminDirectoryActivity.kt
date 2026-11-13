package com.dur1al.controlate.porteria.ui.db

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityAdminDirectoryBinding
import com.dur1al.controlate.databinding.BottomSheetEntradaBinding
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.validation.Rut
import com.dur1al.controlate.porteria.ui.common.applyRutFormatter
import com.dur1al.controlate.porteria.ui.common.collectWhenStarted
import com.dur1al.controlate.porteria.ui.common.textTrimmed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Directorio editable (admin): tocar para editar, mantener presionado para eliminar. */
class AdminDirectoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDirectoryBinding
    private val viewModel: DirectoryViewModel by viewModel()
    private val adapter = PersonaAdapter(onEdit = ::showEditDialog, onDelete = ::confirmDelete)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDirectoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.fabImport.isVisible = false
        binding.rvDirectory.layoutManager = LinearLayoutManager(this)
        binding.rvDirectory.adapter = adapter
        binding.etSearch.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }

        collectWhenStarted(viewModel.results) { list ->
            adapter.submitList(list.orEmpty())
            binding.tvEmpty.isVisible = list != null && list.isEmpty()
        }
        collectWhenStarted(viewModel.events) { event ->
            if (event is DirectoryViewModel.Event.Message) {
                Snackbar.make(binding.root, event.message.resolve(this), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(persona: Persona) {
        val form = BottomSheetEntradaBinding.inflate(layoutInflater).apply {
            tvTitle.setText(R.string.directory_edit_title)
            btnGuardar.isVisible = false
            cbEsPadron.isVisible = false
            tilPadron.isVisible = !persona.padron.isNullOrBlank()
            etRut.applyRutFormatter()
            etPatente.setText(persona.patente)
            etNombre.setText(persona.nombreApellido)
            etDepto.setText(persona.departamento)
            etEmpresa.setText(persona.empresa)
            etRut.setText(persona.rut?.let(Rut::format))
            etPadron.setText(persona.padron)
            etAuto.setText(persona.descripcionAuto)
            when (PersonaType.fromCode(persona.tipoPersona)) {
                PersonaType.ESTUDIANTE -> rbEstudiante.isChecked = true
                PersonaType.FUNCIONARIO -> rbFuncionario.isChecked = true
                else -> rbVisita.isChecked = true
            }
        }
        MaterialAlertDialogBuilder(this)
            .setView(form.root)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                viewModel.update(
                    persona.copy(
                        patente = form.etPatente.textTrimmed(),
                        nombreApellido = form.etNombre.textTrimmed(),
                        departamento = form.etDepto.textTrimmed(),
                        empresa = form.etEmpresa.textTrimmed().ifEmpty { null },
                        rut = form.etRut.textTrimmed(),
                        padron = form.etPadron.textTrimmed().ifEmpty { null },
                        descripcionAuto = form.etAuto.textTrimmed(),
                        tipoPersona = when (form.rgTipoPersona.checkedRadioButtonId) {
                            form.rbEstudiante.id -> PersonaType.ESTUDIANTE
                            form.rbFuncionario.id -> PersonaType.FUNCIONARIO
                            else -> PersonaType.VISITA
                        }.name
                    )
                )
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun confirmDelete(persona: Persona) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.directory_delete_title)
            .setMessage(getString(R.string.directory_delete_message, persona.nombreApellido))
            .setPositiveButton(R.string.dialog_delete) { _, _ -> viewModel.delete(persona) }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
