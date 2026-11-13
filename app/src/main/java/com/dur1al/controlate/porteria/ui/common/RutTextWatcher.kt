package com.dur1al.controlate.porteria.ui.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.dur1al.controlate.porteria.domain.validation.RutInputFormatter

/**
 * Aplica [RutInputFormatter] al escribir. Toda la lógica vive en el formateador (probado en la
 * JVM); aquí solo se detecta el borrado de un separador y se usa el cursor posterior a la edición.
 */
class RutTextWatcher(private val editText: EditText) : TextWatcher {

    private var isUpdating = false
    private var deletedSeparator = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        if (isUpdating) return
        deletedSeparator = s != null && count == 1 && after == 0 &&
            start < s.length && !RutInputFormatter.isSignificant(s[start])
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(s: Editable?) {
        if (isUpdating || s == null) return
        val current = s.toString()
        val result = RutInputFormatter.format(current, editText.selectionEnd.coerceAtLeast(0), deletedSeparator)
        deletedSeparator = false
        if (result.text == current && result.cursor == editText.selectionEnd) return

        isUpdating = true
        // `replace` conserva los listeners del Editable (a diferencia de `setText`).
        s.replace(0, s.length, result.text)
        editText.setSelection(result.cursor.coerceIn(0, s.length))
        isUpdating = false
    }
}
