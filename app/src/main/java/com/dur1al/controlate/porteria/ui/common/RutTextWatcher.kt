package com.dur1al.controlate.porteria.ui.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.dur1al.controlate.porteria.domain.validation.Rut

class RutTextWatcher(
    private val editText: EditText,
    private val maxRutLength: Int = 9
) : TextWatcher {
    private var isUpdating = false
    private var selectionBeforeChange = 0

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        selectionBeforeChange = editText.selectionStart.coerceAtLeast(0)
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        val currentText = s?.toString().orEmpty()
        if (isUpdating || currentText.isEmpty()) return

        val clean = Rut.normalize(currentText)
            .uppercase()
            .take(maxRutLength)
        val formatted = Rut.format(clean).uppercase()

        if (currentText == formatted) return

        val cleanBeforeCursor = Rut.normalize(
            currentText.take(selectionBeforeChange.coerceAtMost(currentText.length))
        ).length.coerceAtMost(clean.length)

        val newSelection = positionForCleanIndex(formatted, cleanBeforeCursor)
        isUpdating = true
        editText.setText(formatted)
        editText.setSelection(newSelection.coerceIn(0, formatted.length))
        isUpdating = false
    }

    private fun positionForCleanIndex(formatted: String, cleanIndex: Int): Int {
        if (cleanIndex <= 0) return 0
        var seen = 0
        for ((index, ch) in formatted.withIndex()) {
            if (ch.isDigit() || ch == 'K') {
                seen++
            }
            if (seen >= cleanIndex) {
                return index + 1
            }
        }
        return formatted.length
    }
}
