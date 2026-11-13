package com.dur1al.controlate.porteria.ui.admin

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivityControlPointsBinding
import com.dur1al.controlate.databinding.DialogControlPointBinding
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.usecase.ControlPointNameError
import com.dur1al.controlate.porteria.ui.common.collectWhenStarted
import com.dur1al.controlate.porteria.ui.common.textTrimmed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Puntos de control (admin): lista, alta, renombre y activación. */
class ControlPointsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControlPointsBinding
    private val viewModel: ControlPointsViewModel by viewModel()
    private val adapter = ControlPointAdapter(
        onRename = ::showRenameDialog,
        onToggle = { point, active -> viewModel.setActive(point.id, active) }
    )

    private var dialogForm: DialogControlPointBinding? = null
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityControlPointsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvPoints.layoutManager = LinearLayoutManager(this)
        binding.rvPoints.adapter = adapter
        binding.fabAddPoint.setOnClickListener { showAddDialog() }

        collectWhenStarted(viewModel.state) { state ->
            adapter.submitList(state.points)
            binding.tvEmpty.isVisible = !state.loading && state.points.isEmpty()
        }
        collectWhenStarted(viewModel.nameError) { error ->
            dialogForm?.tilName?.error = error?.let { getString(it.messageRes) }
        }
        collectWhenStarted(viewModel.saving) { saving ->
            dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !saving
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                ControlPointsViewModel.Event.DialogDone -> dialog?.dismiss()
                is ControlPointsViewModel.Event.Message ->
                    Snackbar.make(binding.root, event.message.resolve(this), Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.fabAddPoint)
                        .show()
            }
        }
    }

    private fun showAddDialog() {
        val form = DialogControlPointBinding.inflate(layoutInflater)
        fun suggestion(kind: ControlPoint.Kind) = getString(
            if (kind == ControlPoint.Kind.BICICLETERO) R.string.control_points_default_bicicletero else R.string.control_points_default_porteria,
            viewModel.state.value.nextNumber(kind)
        )
        fun selectedKind() =
            if (form.toggleKind.checkedButtonId == form.btnKindBicicletero.id) ControlPoint.Kind.BICICLETERO else ControlPoint.Kind.PORTERIA

        form.toggleKind.check(form.btnKindPorteria.id)
        form.etName.setText(suggestion(ControlPoint.Kind.PORTERIA))
        // Al cambiar el tipo se actualiza la sugerencia, salvo que el admin ya haya escrito un nombre propio.
        form.toggleKind.addOnButtonCheckedListener { _, _, checked ->
            if (!checked) return@addOnButtonCheckedListener
            val current = form.etName.textTrimmed()
            if (current.isEmpty() || ControlPoint.Kind.entries.any { current == suggestion(it) }) {
                form.etName.setText(suggestion(selectedKind()))
            }
        }
        openDialog(form, R.string.control_points_add_title) { viewModel.add(selectedKind(), form.etName.textTrimmed()) }
    }

    private fun showRenameDialog(point: ControlPoint) {
        val form = DialogControlPointBinding.inflate(layoutInflater)
        // El tipo no se puede cambiar: los turnos ya registrados dependen de él.
        form.tvKindLabel.isVisible = false
        form.toggleKind.isVisible = false
        form.etName.setText(point.name)
        openDialog(form, R.string.control_points_rename_title) { viewModel.rename(point.id, form.etName.textTrimmed()) }
    }

    /** Diálogo que no se cierra solo: con un nombre inválido queda abierto con el error en el campo. */
    private fun openDialog(form: DialogControlPointBinding, titleRes: Int, onSave: () -> Unit) {
        viewModel.startDialog()
        form.etName.doAfterTextChanged { viewModel.clearNameError() }
        form.etName.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_DONE).also { if (it) onSave() }
        }
        form.etName.setSelection(form.etName.text?.length ?: 0)
        dialogForm = form
        val shown = MaterialAlertDialogBuilder(this)
            .setTitle(titleRes)
            .setView(form.root)
            .setPositiveButton(R.string.dialog_save, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setOnDismissListener {
                dialogForm = null
                dialog = null
            }
            .show()
        shown.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onSave() }
        dialog = shown
        form.etName.requestFocus()
    }

    private val ControlPointNameError.messageRes: Int
        get() = when (this) {
            ControlPointNameError.REQUIRED -> R.string.error_point_name_required
            ControlPointNameError.TOO_SHORT -> R.string.error_point_name_too_short
            ControlPointNameError.TOO_LONG -> R.string.error_point_name_too_long
            ControlPointNameError.INVALID_CHARS -> R.string.error_point_name_invalid
            ControlPointNameError.DUPLICATE -> R.string.error_point_name_duplicate
        }
}
