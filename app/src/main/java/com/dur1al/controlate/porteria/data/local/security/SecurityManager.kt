package com.dur1al.controlate.porteria.data.local.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Gestiona el PIN de administrador de forma cifrada mediante [EncryptedSharedPreferences]
 * y almacenamiento derivado con [PinSecurityHelper] (PBKDF2WithHmacSHA256 + salt).
 *
 * No incluye credenciales por defecto en código fuente; requiere configuración en el primer uso.
 */
class SecurityManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "controlate_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isAdminPinConfigured(): Boolean {
        return !sharedPreferences.getString(KEY_ADMIN_PIN_HASH, null).isNullOrBlank()
    }

    fun setAdminPin(newPin: String): Boolean {
        if (!PinSecurityHelper.isValidPinFormat(newPin)) return false
        val hashed = PinSecurityHelper.hashPin(newPin)
        sharedPreferences.edit()
            .putString(KEY_ADMIN_PIN_HASH, hashed)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
        return true
    }

    fun isLockedOut(nowMs: Long = System.currentTimeMillis()): Boolean {
        return getRemainingLockoutSeconds(nowMs) > 0L
    }

    fun getRemainingLockoutSeconds(nowMs: Long = System.currentTimeMillis()): Long {
        val lockoutUntil = sharedPreferences.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (lockoutUntil <= nowMs) return 0L
        return ((lockoutUntil - nowMs) + 999L) / 1000L
    }

    fun validateAdminPin(input: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (input.isBlank() || isLockedOut(nowMs)) return false
        val storedHash = sharedPreferences.getString(KEY_ADMIN_PIN_HASH, null) ?: return false

        return if (PinSecurityHelper.verifyPin(input, storedHash)) {
            sharedPreferences.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            true
        } else {
            val attempts = sharedPreferences.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val editor = sharedPreferences.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                editor.putLong(KEY_LOCKOUT_UNTIL, nowMs + LOCKOUT_DURATION_MS)
                editor.putInt(KEY_FAILED_ATTEMPTS, 0)
            }
            editor.apply()
            false
        }
    }

    companion object {
        private const val KEY_ADMIN_PIN_HASH = "ADMIN_PIN_HASH"
        private const val KEY_FAILED_ATTEMPTS = "ADMIN_FAILED_ATTEMPTS"
        private const val KEY_LOCKOUT_UNTIL = "ADMIN_LOCKOUT_UNTIL"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L
    }
}
