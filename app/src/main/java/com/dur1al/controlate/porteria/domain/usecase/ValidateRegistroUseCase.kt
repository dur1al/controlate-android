package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.domain.model.BikeEntryInput
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.model.ValidationError.Field
import com.dur1al.controlate.porteria.domain.model.ValidationError.Reason
import com.dur1al.controlate.porteria.domain.model.VehicleEntryInput
import com.dur1al.controlate.porteria.domain.validation.Patente
import com.dur1al.controlate.porteria.domain.validation.Rut

/**
 * Reglas de validación de los formularios de ingreso. Función pura: sin I/O ni Android.
 * Devuelve una lista vacía cuando la entrada es válida.
 */
class ValidateRegistroUseCase {

    fun validateVehicle(input: VehicleEntryInput): List<ValidationError> = buildList {
        if (!Patente.isValid(input.patente)) add(ValidationError(Field.PATENTE, Reason.INVALID_FORMAT))
        validateName(input.nombre)?.let(::add)
        if (!input.rut.isNullOrBlank() && !Rut.isValid(input.rut)) {
            add(ValidationError(Field.RUT, Reason.INVALID_FORMAT))
        }
        if (input.esPadron && input.padron.isNullOrBlank()) add(ValidationError(Field.PADRON, Reason.REQUIRED))
    }

    fun validateBike(input: BikeEntryInput): List<ValidationError> = buildList {
        when {
            input.rut.isBlank() -> add(ValidationError(Field.RUT, Reason.REQUIRED))
            !Rut.isValid(input.rut) -> add(ValidationError(Field.RUT, Reason.INVALID_FORMAT))
        }
        validateName(input.nombre)?.let(::add)
        if (input.carrera.isBlank()) add(ValidationError(Field.CARRERA, Reason.REQUIRED))
    }

    private fun validateName(nombre: String): ValidationError? {
        val trimmed = nombre.trim()
        return when {
            trimmed.isEmpty() -> ValidationError(Field.NOMBRE, Reason.REQUIRED)
            trimmed.length < MIN_NAME_LENGTH -> ValidationError(Field.NOMBRE, Reason.TOO_SHORT)
            else -> null
        }
    }

    private companion object {
        const val MIN_NAME_LENGTH = 2
    }
}
