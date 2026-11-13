package com.dur1al.controlate.porteria.domain.model

/**
 * Tipos de persona admitidos.
 *
 * Se persiste el [name] (código estable, ej. `FUNCIONARIO`). El texto visible se resuelve
 * en la capa de presentación desde `strings.xml`, así el dominio no depende de un idioma.
 */
enum class PersonaType {
    VISITA,
    ESTUDIANTE,
    FUNCIONARIO,
    PROVEEDOR,
    OTRO;

    companion object {
        /**
         * Resuelve un código o un nombre visible heredado (`"Funcionario"`), sin distinguir mayúsculas.
         * Vacío -> [VISITA]; desconocido -> [OTRO].
         */
        fun fromCode(code: String?): PersonaType {
            val value = code?.trim().orEmpty()
            if (value.isEmpty()) return VISITA
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTRO
        }
    }
}
