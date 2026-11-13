package com.dur1al.controlate.porteria.domain.validation

/**
 * Única fuente de verdad para el RUT chileno.
 *
 * La forma canónica que se persiste es `cuerpo + DV` sin puntos ni guion y con `K`
 * en mayúscula (ej: `7654321K`). Toda búsqueda, validación y almacenamiento debe pasar
 * por [normalize] para que los RUT sean comparables entre pantallas.
 */
object Rut {

    private const val MIN_BODY_DIGITS = 6
    private const val MAX_BODY_DIGITS = 8

    /** Largo máximo de la forma canónica (cuerpo + DV). */
    const val MAX_LENGTH = MAX_BODY_DIGITS + 1

    /**
     * Formatos aceptados al escribir: `12345678-5`, `123456785` o `12.345.678-5`
     * (puntos solo cada 3 dígitos y en todo el cuerpo). La `K` solo puede ser el DV.
     */
    private val ACCEPTED_FORMAT = Regex("^(\\d{1,3}(\\.\\d{3})+|\\d+)-?[\\dK]$")

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

    /**
     * Valida formato, largo y dígito verificador (módulo 11). Nunca lanza excepción.
     *
     * Es estricto con lo que escribe la persona: rechaza letras en el cuerpo, puntos mal
     * ubicados o cuerpos que empiezan en 0, en vez de "limpiarlos" y validar otro RUT.
     */
    fun isValid(raw: String): Boolean {
        val trimmed = raw.trim().uppercase()
        if (!ACCEPTED_FORMAT.matches(trimmed)) return false
        val clean = normalize(trimmed)
        val body = clean.dropLast(1)
        if (body.length !in MIN_BODY_DIGITS..MAX_BODY_DIGITS || body.first() == '0') return false
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

    /** Dígito verificador para un cuerpo numérico (útil para generar datos de prueba sintéticos). */
    fun computeCheckDigit(body: String): Char {
        require(body.isNotEmpty() && body.all { it.isDigit() }) { "El cuerpo del RUT debe ser numérico" }
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
