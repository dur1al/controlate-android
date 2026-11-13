package com.dur1al.controlate.porteria.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dur1al.controlate.databinding.ActivitySettingsBinding
import com.dur1al.controlate.porteria.data.local.security.SecurityManager
import org.koin.android.ext.android.inject

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val securityManager: SecurityManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        binding.btnSavePin.setOnClickListener {
            val currentPin = binding.etCurrentPin.text.toString()
            val newPin = binding.etNewPin.text.toString()

            if (securityManager.isAdminPinConfigured()) {
                if (currentPin.isBlank() || newPin.isBlank()) {
                    Toast.makeText(this, "Debe llenar ambos campos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (securityManager.isLockedOut()) {
                    Toast.makeText(
                        this,
                        "Demasiados intentos. Espera ${securityManager.getRemainingLockoutSeconds()} s",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (!securityManager.validateAdminPin(currentPin)) {
                    binding.tilCurrentPin.error = "PIN actual incorrecto"
                    return@setOnClickListener
                }
                binding.tilCurrentPin.error = null
            } else if (newPin.isBlank()) {
                Toast.makeText(this, "Ingresa el nuevo PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!securityManager.setAdminPin(newPin)) {
                binding.tilNewPin.error = "El PIN debe tener entre 4 y 8 dígitos numéricos"
                return@setOnClickListener
            }
            binding.tilNewPin.error = null

            Toast.makeText(this, "PIN actualizado exitosamente", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
