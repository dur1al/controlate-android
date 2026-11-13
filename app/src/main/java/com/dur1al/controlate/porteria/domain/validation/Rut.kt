package com.dur1al.controlate.porteria.domain.validation

/**
 * Única fuente de verdad para el RUT chileno.
 *
 * La forma canónica que se persiste es `cuerpo + DV` sin puntos ni guion y con `K`
 * en mayúscula (ej: `7654321K`). Toda búsqueda, validación y almacenamiento debe pasar
 * por [normalize] para que los RUT sean comparables entre pantallas.
 */
object Rut {

    private const val MIN_LENGTH = 7
    private const val MAX_LENGTH = 9

    /** Devuelve la forma canónica: solo dígitos y, si existe, una `K` final como dígito verificador. */
    fun normalize(raw: String): String {
        val chars = raw.uppercase().filter { it.isDigit() || it == 'K' }
        if (chars.isEmpty()) return ""
        val body = chars.dropLast(1).filter { it.isDigit() }
        return body + chars.last()
    }

    /** Igual que [normalize], pero devuelve `null` si no queda nada útil. */
    fun normalizeOrNull(raw: String?): String? =
        raw?.let(::normalize)?.takeIf { it.isNotEmpty() }

    /** Valida largo y dígito verificador (módulo 11). Nunca lanza excepción. */
    fun isValid(raw: String): Boolean {
        val clean = normalize(raw)
        if (clean.length !in MIN_LENGTH..MAX_LENGTH) return false
        val body = clean.dropLast(1)
        if (body.any { !it.isDigit() }) return false
        return computeCheckDigit(body) == clean.last()
    }

    /** Formatea como `12.345.678-5`. Entradas de menos de 2 caracteres se devuelven normalizadas. */
    fun format(raw: String): String {
        val clean = normalize(raw)
        if (clean.length < 2) return clean
        val body = clean.dropLast(1)
        val grouped = body.reversed().chunked(3).joinToString(".").reversed()
        return "$grouped-${clean.last()}"
    }

    private fun computeCheckDigit(body: String): Char {
        var sum = 0
        var factor = 2
        for (digit in body.reversed()) {
            sum += digit.digitToInt() * factor
            factor = if (factor == 7) 2 else factor + 1
        }
        return when (val dv = 11 - (sum % 11)) {
            11 -> '0'
            10 -> 'K'
            else -> dv.digitToChar()
        }
    }
}
