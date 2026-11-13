package com.dur1al.controlate.porteria.data.local.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Utilidad criptográfica pura (sin dependencias de Android) para validar,
 * derivar y verificar hashes de PIN de administrador usando PBKDF2WithHmacSHA256.
 */
object PinSecurityHelper {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val MIN_PIN_LENGTH = 4
    private const val MAX_PIN_LENGTH = 8

    fun isValidPinFormat(pin: String): Boolean {
        return pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it.isDigit() }
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun hashPin(pin: String, salt: ByteArray = generateSalt()): String {
        require(isValidPinFormat(pin)) { "Formato de PIN inválido" }
        val derived = deriveKey(pin.toCharArray(), salt)
        val encoder = Base64.getEncoder()
        return "${encoder.encodeToString(salt)}:${encoder.encodeToString(derived)}"
    }

    fun verifyPin(pin: String, storedHash: String?): Boolean {
        if (pin.isBlank() || storedHash.isNullOrBlank()) return false
        val parts = storedHash.split(':')
        if (parts.size != 2) return false

        return runCatching {
            val decoder = Base64.getDecoder()
            val salt = decoder.decode(parts[0])
            val expectedHash = decoder.decode(parts[1])
            val candidateHash = deriveKey(pin.toCharArray(), salt)
            MessageDigest.isEqual(candidateHash, expectedHash)
        }.getOrDefault(false)
    }

    private fun deriveKey(chars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
