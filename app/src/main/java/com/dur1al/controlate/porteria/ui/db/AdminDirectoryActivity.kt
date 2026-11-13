package com.dur1al.controlate.porteria.ui.db

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityAdminDirectoryBinding
import com.dur1al.controlate.databinding.BottomSheetEntradaBinding
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.validation.Rut
import com.dur1al.controlate.porteria.ui.common.applyRutFormatter
import com.dur1al.controlate.porteria.ui.common.collectWhenStarted
import com.dur1al.controlate.porteria.ui.common.focusFirstError
import com.dur1al.controlate.porteria.ui.common.messageRes
import com.dur1al.controlate.porteria.ui.common.textTrimmed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Directorio editable (admin): tocar para editar, mantener presionado para eliminar. */
class AdminDirectoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDirectoryBinding
    private val viewModel: DirectoryViewModel by viewModel()
    private val adapter = PersonaAdapter(onEdit = ::showEditDialog, onDelete = ::confirmDelete)

    /** Formulario y diálogo de edición abiertos (null si no hay edición en curso). */
    private var editForm: BottomSheetEntradaBinding? = null
    private var editDialog: AlertDialog? = null

    /** Tras pulsar "Guardar", el siguiente render de errores lleva el foco al primero. */
    private var focusErrorOnRender = false

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
        collectWhenStarted(viewModel.editErrors, ::showEditErrors)
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is DirectoryViewModel.Event.Message ->
                    Snackbar.make(binding.root, event.message.resolve(this), Snackbar.LENGTH_SHORT).show()
                DirectoryViewModel.Event.EditSaved -> editDialog?.dismiss()
                else -> Unit
            }
        }
    }

    private fun showEditDialog(persona: Persona) {
        viewModel.startEdit()
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
            clearErrorOnEdit(etPatente, ValidationError.Field.PATENTE)
            clearErrorOnEdit(etNombre, ValidationError.Field.NOMBRE)
            clearErrorOnEdit(etRut, ValidationError.Field.RUT)
        }
        editForm = form
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(form.root)
            .setPositiveButton(R.string.dialog_save, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setOnDismissListener {
                editForm = null
                editDialog = null
            }
            .show()
        // El listener propio evita que el diálogo se cierre solo: con errores sigue abierto y no se pierde lo escrito.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            focusErrorOnRender = true
            viewModel.update(readPersona(form, persona))
        }
        editDialog = dialog
    }

    private fun clearErrorOnEdit(field: EditText, which: ValidationError.Field) {
        field.doAfterTextChanged { if (editForm != null) viewModel.clearEditError(which) }
    }

    private fun readPersona(form: BottomSheetEntradaBinding, persona: Persona) = persona.copy(
        patente = form.etPatente.textTrimmed(),
        nombreApellido = form.etNombre.textTrimmed(),
        departamento = form.etDepto.textTrimmed(),
        empresa = form.etEmpresa.textTrimmed().ifEmpty { null },
        rut = form.etRut.textTrimmed().ifEmpty { null },
        padron = form.etPadron.textTrimmed().ifEmpty { null },
        descripcionAuto = form.etAuto.textTrimmed(),
        tipoPersona = when (form.rgTipoPersona.checkedRadioButtonId) {
            form.rbEstudiante.id -> PersonaType.ESTUDIANTE
            form.rbFuncionario.id -> PersonaType.FUNCIONARIO
            else -> PersonaType.VISITA
        }.name
    )

    private fun showEditErrors(errors: List<ValidationError>) {
        val form = editForm ?: return
        val byField = errors.associateBy { it.field }
        form.tilPatente.error = byField[ValidationError.Field.PATENTE]?.let { getString(it.messageRes) }
        form.tilNombre.error = byField[ValidationError.Field.NOMBRE]?.let { getString(it.messageRes) }
        form.tilRut.error = byField[ValidationError.Field.RUT]?.let { getString(it.messageRes) }
        if (focusErrorOnRender && errors.isNotEmpty()) {
            focusErrorOnRender = false
            focusFirstError(listOf(form.tilPatente, form.tilNombre, form.tilRut))
        }
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
