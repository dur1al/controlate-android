package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.domain.model.Result

/**
 * Validaciones centralizadas para registros de ingreso.
 * Encapsula las reglas de negocio que antes estaban dispersas
 * entre [EntradaBottomSheet], [BicicletaBottomSheet] y otros.
 */
class ValidateRegistroUseCase {

    /** Resultado de validación con campo y mensaje de error. */
    data class ValidationError(val field: Field, val message: String)

    enum class Field {
        PATENTE, NOMBRE, RUT, PADRON, DEPTO, CARRERA
    }

    /**
     * Valida los campos de un registro de portería.
     * @return [Result.Success] con Unit si es válido, [Result.Error] con los errores.
     */
    fun validatePorteriaEntry(
        patente: String,
        nombre: String,
        rut: String?,
        esPadron: Boolean,
        padron: String?
    ): Result<List<ValidationError>> {
        val errors = mutableListOf<ValidationError>()

        val cleanPatente = patente.replace(" ", "").uppercase()
        if (cleanPatente.length < 4 || cleanPatente.length > 6) {
            errors.add(ValidationError(Field.PATENTE, "La patente debe tener entre 4 y 6 caracteres"))
        } else if (!cleanPatente.matches(Regex("^[A-Z0-9]+\$"))) {
            errors.add(ValidationError(Field.PATENTE, "La patente solo debe contener letras y números (sin guiones)"))
        }

        if (nombre.isBlank()) {
            errors.add(ValidationError(Field.NOMBRE, "El nombre es obligatorio"))
        } else if (nombre.length < 2) {
            errors.add(ValidationError(Field.NOMBRE, "El nombre debe tener al menos 2 caracteres"))
        }

        if (!rut.isNullOrBlank() && rut.length >= 7) {
            if (!isValidRut(rut)) {
                errors.add(ValidationError(Field.RUT, "El RUT ingresado no es válido"))
            }
        }

        if (esPadron && padron.isNullOrBlank()) {
            errors.add(ValidationError(Field.PADRON, "El número de padrón es obligatorio"))
        }

        return if (errors.isEmpty()) {
            Result.Success(emptyList())
        } else {
            Result.Success(errors)
        }
    }

    /**
     * Valida los campos de un registro de bicicletero.
     */
    fun validateBicicletaEntry(
        rut: String,
        nombre: String,
        carrera: String
    ): Result<List<ValidationError>> {
        val errors = mutableListOf<ValidationError>()

        val cleanRut = cleanRut(rut)
        if (cleanRut.length < 7) {
            errors.add(ValidationError(Field.RUT, "El RUT debe tener al menos 7 dígitos"))
        } else if (!isValidRut(cleanRut)) {
            errors.add(ValidationError(Field.RUT, "El RUT ingresado no es válido"))
        }

        if (nombre.isBlank()) {
            errors.add(ValidationError(Field.NOMBRE, "El nombre es obligatorio"))
        }

        if (carrera.isBlank()) {
            errors.add(ValidationError(Field.CARRERA, "La carrera es obligatoria"))
        }

        return Result.Success(errors)
    }

    // region RUT Validation (internalized from RutUtils)

    private fun cleanRut(rut: String): String {
        return rut.replace(Regex("[^0-9kK]"), "").uppercase()
    }

    private fun isValidRut(rut: String): Boolean {
        val clean = cleanRut(rut)
        if (clean.length < 2) return false
        val cuerpo = clean.substring(0, clean.length - 1)
        val dv = clean.last().uppercaseChar()

        var m = 0
        var s = 1
        var i = cuerpo.length - 1
        while (i >= 0) {
            s = (s + cuerpo[i].toString().toInt() * (9 - m % 6)) % 11
            m++
            i--
        }
        val expectedDv = if (s > 0) (s - 1).toString() else "K"
        return dv.toString() == expectedDv
    }

    // endregion
}
