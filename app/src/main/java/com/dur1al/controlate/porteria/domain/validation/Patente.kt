package com.dur1al.controlate.porteria.domain.validation

/** Normalización y validación de patentes vehiculares (formato chileno, 4 a 6 caracteres). */
object Patente {

    private val VALID_PATTERN = Regex("^[A-Z0-9]{4,6}$")

    fun normalize(raw: String): String =
        raw.uppercase().filterNot { it.isWhitespace() || it == '-' || it == '.' }

    fun isValid(raw: String): Boolean = VALID_PATTERN.matches(normalize(raw))
}
