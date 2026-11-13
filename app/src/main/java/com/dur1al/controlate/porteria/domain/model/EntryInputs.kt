package com.dur1al.controlate.porteria.domain.model

/** Datos capturados en el formulario de ingreso vehicular, antes de validar y persistir. */
data class VehicleEntryInput(
    val patente: String,
    val nombre: String,
    val rut: String?,
    val departamento: String,
    val descripcionAuto: String,
    val empresa: String?,
    val tipoPersona: PersonaType,
    val esPadron: Boolean,
    val padron: String?,
    val observaciones: String?
)

/** Datos capturados en el formulario de ingreso al bicicletero. */
data class BikeEntryInput(
    val rut: String,
    val nombre: String,
    val carrera: String,
    val tipoPersona: PersonaType,
    val observacion: String?
)

/**
 * Error de validación expresado como códigos, no como texto: la UI decide el mensaje
 * (`strings.xml`), y los tests comparan valores estables.
 */
data class ValidationError(val field: Field, val reason: Reason) {
    enum class Field { PATENTE, NOMBRE, RUT, PADRON, CARRERA }
    enum class Reason {
        REQUIRED, TOO_SHORT, TOO_LONG, INVALID_FORMAT,

        /** Ya hay un ingreso sin salida con ese dato en el turno. */
        ALREADY_INSIDE,

        /** Otro registro del directorio ya usa ese dato. */
        DUPLICATE
    }
}
