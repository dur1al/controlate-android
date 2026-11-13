package com.dur1al.controlate.porteria.utils

import android.widget.EditText

fun EditText.applyRutFormatter(maxRutLength: Int = 9) {
    this.addTextChangedListener(RutTextWatcher(this, maxRutLength))
}
