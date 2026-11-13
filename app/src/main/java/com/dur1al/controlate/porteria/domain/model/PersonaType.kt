package com.dur1al.controlate.porteria.domain.model

/**
 * Tipos de persona admitidos en el sistema.
 * Vinculados a las opciones del formulario de registro.
 */
enum class PersonaType(val displayName: String) {
    VISITA("Visita"),
    ESTUDIANTE("Estudiante"),
    FUNCIONARIO("Funcionario"),
    PROVEEDOR("Proveedor"),
    OTRO("Otro");

    companion object {
        /** Resuelve un tipo a partir de su nombre visible (case-insensitive). */
        fun fromDisplayName(name: String?): PersonaType {
            if (name.isNullOrBlank()) return VISITA
            return entries.find { it.displayName.equals(name, ignoreCase = true) } ?: OTRO
        }
    }
}
