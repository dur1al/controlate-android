package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.domain.model.BikeEntryInput
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.model.ValidationError.Field
import com.dur1al.controlate.porteria.domain.model.ValidationError.Reason
import com.dur1al.controlate.porteria.domain.model.VehicleEntryInput
import com.dur1al.controlate.porteria.domain.validation.Patente
import com.dur1al.controlate.porteria.domain.validation.Rut

/**
 * Reglas de validación de los formularios. Función pura: sin I/O ni Android.
 * Devuelve una lista vacía cuando la entrada es válida.
 *
 * Las reglas por campo son públicas para validar al salir de cada campo (feedback temprano)
 * con exactamente el mismo criterio que al guardar.
 */
class ValidateRegistroUseCase {

    fun validateVehicle(input: VehicleEntryInput): List<ValidationError> = listOfNotNull(
        patente(input.patente),
        nombre(input.nombre),
        rut(input.rut, required = false),
        if (input.esPadron && input.padron.isNullOrBlank()) ValidationError(Field.PADRON, Reason.REQUIRED) else null
    )

    fun validateBike(input: BikeEntryInput): List<ValidationError> = listOfNotNull(
        rut(input.rut, required = true),
        nombre(input.nombre),
        carrera(input.carrera)
    )

    /** Edición de una persona del directorio (admin): patente y nombre obligatorios, RUT opcional. */
    fun validatePersona(patente: String, nombre: String, rut: String?): List<ValidationError> =
        listOfNotNull(patente(patente), nombre(nombre), rut(rut, required = false))

    fun patente(raw: String): ValidationError? = when {
        raw.isBlank() -> ValidationError(Field.PATENTE, Reason.REQUIRED)
        !Patente.isValid(raw) -> ValidationError(Field.PATENTE, Reason.INVALID_FORMAT)
        else -> null
    }

    fun rut(raw: String?, required: Boolean): ValidationError? = when {
        raw.isNullOrBlank() -> if (required) ValidationError(Field.RUT, Reason.REQUIRED) else null
        !Rut.isValid(raw) -> ValidationError(Field.RUT, Reason.INVALID_FORMAT)
        else -> null
    }

    fun nombre(raw: String): ValidationError? {
        val trimmed = raw.trim()
        return when {
            trimmed.isEmpty() -> ValidationError(Field.NOMBRE, Reason.REQUIRED)
            trimmed.length < MIN_NAME_LENGTH -> ValidationError(Field.NOMBRE, Reason.TOO_SHORT)
            trimmed.length > MAX_NAME_LENGTH -> ValidationError(Field.NOMBRE, Reason.TOO_LONG)
            !NAME_PATTERN.matches(trimmed) -> ValidationError(Field.NOMBRE, Reason.INVALID_FORMAT)
            else -> null
        }
    }

    fun carrera(raw: String): ValidationError? = when {
        raw.isBlank() -> ValidationError(Field.CARRERA, Reason.REQUIRED)
        raw.trim().length > MAX_TEXT_LENGTH -> ValidationError(Field.CARRERA, Reason.TOO_LONG)
        else -> null
    }

    companion object {
        const val MIN_NAME_LENGTH = 2
        const val MAX_NAME_LENGTH = 60
        const val MAX_TEXT_LENGTH = 60

        /** Letras (con tildes y ñ), espacios, apóstrofes, puntos y guiones; al menos una letra. */
        private val NAME_PATTERN = Regex("^(?=.*\\p{L})[\\p{L} .'’-]+$")
    }
}
