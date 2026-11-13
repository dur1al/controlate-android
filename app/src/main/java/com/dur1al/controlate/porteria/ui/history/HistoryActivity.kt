package com.dur1al.controlate.porteria.ui.history

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dur1al.controlate.R
import com.google.android.material.appbar.MaterialToolbar

/**
 * Pantalla que muestra el historial de registros de un turno específico.
 */
class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}
