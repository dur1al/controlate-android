package com.dur1al.controlate.porteria.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ActivitySettingsBinding
import com.dur1al.controlate.porteria.ui.common.collectWhenStarted
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Ajustes del administrador: cambio de PIN. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.etCurrentPin.doAfterTextChanged { binding.tilCurrentPin.error = null }
        binding.etNewPin.doAfterTextChanged { binding.tilNewPin.error = null }
        binding.btnSavePin.setOnClickListener {
            viewModel.changePin(binding.etCurrentPin.text.toString(), binding.etNewPin.text.toString())
        }

        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                SettingsViewModel.Event.PinChanged -> {
                    Snackbar.make(binding.root, R.string.settings_pin_updated, Snackbar.LENGTH_SHORT).show()
                    binding.etCurrentPin.text = null
                    binding.etNewPin.text = null
                }
                SettingsViewModel.Event.WrongCurrentPin ->
                    binding.tilCurrentPin.error = getString(R.string.settings_pin_current_wrong)
                SettingsViewModel.Event.InvalidNewPin ->
                    binding.tilNewPin.error = getString(R.string.login_admin_pin_invalid_format)
                is SettingsViewModel.Event.Message ->
                    Snackbar.make(binding.root, event.message.resolve(this), Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
