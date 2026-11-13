package com.dur1al.controlate.porteria.data.local.security

import com.dur1al.controlate.porteria.domain.time.Clock

/**
 * Política del PIN de administrador: alta en primer uso, verificación y bloqueo temporal.
 *
 * - El PIN se guarda como hash PBKDF2 con salt aleatorio ([PinSecurityHelper]); nunca en claro.
 * - Tras [MAX_FAILED_ATTEMPTS] fallos consecutivos se bloquea durante [LOCKOUT_DURATION_MS].
 * - El almacenamiento es abstracto ([KeyValueStore]); en producción va cifrado con
 *   `EncryptedSharedPreferences`, en tests en memoria.
 *
 * La derivación PBKDF2 es costosa (~100 ms o más): los llamadores deben invocar
 * [verify], [setPin] y [changePin] fuera del hilo principal.
 */
class AdminPinManager(
    private val store: KeyValueStore,
    private val clock: Clock
) {

    sealed interface VerifyResult {
        data object Granted : VerifyResult
        data class Denied(val remainingAttempts: Int) : VerifyResult
        data class LockedOut(val remainingSeconds: Long) : VerifyResult
        data object NotConfigured : VerifyResult
    }

    enum class ChangeResult { Changed, WrongCurrentPin, InvalidNewPin, LockedOut }

    fun isPinConfigured(): Boolean = !store.getString(KEY_PIN_HASH).isNullOrBlank()

    fun isValidFormat(pin: String): Boolean = PinSecurityHelper.isValidPinFormat(pin)

    /** Define el PIN (primer uso). Devuelve `false` si el formato es inválido. */
    fun setPin(newPin: String): Boolean {
        if (!PinSecurityHelper.isValidPinFormat(newPin)) return false
        val hash = PinSecurityHelper.hashPin(newPin)
        store.edit {
            putString(KEY_PIN_HASH, hash)
            putInt(KEY_FAILED_ATTEMPTS, 0)
            putLong(KEY_LOCKOUT_UNTIL, 0L)
        }
        return true
    }

    fun remainingLockoutSeconds(): Long {
        val until = store.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val now = clock.now()
        return if (until <= now) 0L else (until - now + 999) / 1000
    }

    fun verify(pin: String): VerifyResult {
        val hash = store.getString(KEY_PIN_HASH) ?: return VerifyResult.NotConfigured
        remainingLockoutSeconds().takeIf { it > 0 }?.let { return VerifyResult.LockedOut(it) }

        if (PinSecurityHelper.verifyPin(pin, hash)) {
            store.edit {
                putInt(KEY_FAILED_ATTEMPTS, 0)
                putLong(KEY_LOCKOUT_UNTIL, 0L)
            }
            return VerifyResult.Granted
        }

        val attempts = store.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        return if (attempts >= MAX_FAILED_ATTEMPTS) {
            store.edit {
                putInt(KEY_FAILED_ATTEMPTS, 0)
                putLong(KEY_LOCKOUT_UNTIL, clock.now() + LOCKOUT_DURATION_MS)
            }
            VerifyResult.LockedOut(LOCKOUT_DURATION_MS / 1000)
        } else {
            store.edit { putInt(KEY_FAILED_ATTEMPTS, attempts) }
            VerifyResult.Denied(remainingAttempts = MAX_FAILED_ATTEMPTS - attempts)
        }
    }

    fun changePin(currentPin: String, newPin: String): ChangeResult {
        if (isPinConfigured()) {
            when (verify(currentPin)) {
                VerifyResult.Granted, VerifyResult.NotConfigured -> Unit
                is VerifyResult.Denied -> return ChangeResult.WrongCurrentPin
                is VerifyResult.LockedOut -> return ChangeResult.LockedOut
            }
        }
        return if (setPin(newPin)) ChangeResult.Changed else ChangeResult.InvalidNewPin
    }

    private companion object {
        const val KEY_PIN_HASH = "ADMIN_PIN_HASH"
        const val KEY_FAILED_ATTEMPTS = "ADMIN_FAILED_ATTEMPTS"
        const val KEY_LOCKOUT_UNTIL = "ADMIN_LOCKOUT_UNTIL"
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L
    }
}
