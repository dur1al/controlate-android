package com.dur1al.controlate.porteria.ui.common

import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputLayout

/*
 * Apoyo común de los formularios en bottom sheet: abrir completo (el botón principal siempre
 * alcanzable), avisos en línea en vez de snackbars que tapan botones, y foco en el primer error.
 */

/** Abre el sheet expandido y sin estado intermedio: con el teclado abierto no esconde "Registrar". */
fun BottomSheetDialogFragment.expandFully() {
    (dialog as? BottomSheetDialog)?.behavior?.apply {
        skipCollapsed = true
        state = BottomSheetBehavior.STATE_EXPANDED
    }
}

/** Muestra un aviso en línea con el color semántico indicado. */
fun TextView.showFormStatus(message: String, @ColorRes colorRes: Int) {
    text = message
    setTextColor(ContextCompat.getColor(context, colorRes))
    isVisible = true
}

/**
 * Lleva el foco al primer campo con error (en orden visual) para que el guardia vea qué corregir
 * aunque el campo haya quedado fuera de pantalla.
 */
fun focusFirstError(fieldsInOrder: List<TextInputLayout>) {
    val first = fieldsInOrder.firstOrNull { it.isVisible && it.error != null } ?: return
    (first.editText ?: first as View).requestFocus()
}
