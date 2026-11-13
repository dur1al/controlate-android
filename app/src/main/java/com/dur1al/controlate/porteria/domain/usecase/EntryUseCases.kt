package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.domain.model.BikeEntryInput
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.model.VehicleEntryInput
import com.dur1al.controlate.porteria.domain.repository.BikeEntryRepository
import com.dur1al.controlate.porteria.domain.repository.PersonaRepository
import com.dur1al.controlate.porteria.domain.repository.VehicleEntryRepository
import com.dur1al.controlate.porteria.domain.time.Clock
import com.dur1al.controlate.porteria.domain.validation.Patente
import com.dur1al.controlate.porteria.domain.validation.Rut

sealed interface RegisterEntryOutcome {
    data class Saved(val id: Long) : RegisterEntryOutcome
    data class Invalid(val errors: List<ValidationError>) : RegisterEntryOutcome
}

/**
 * Valida, normaliza y guarda un ingreso vehicular. Rechaza una patente que ya está dentro en el turno. Si la persona no existe en el directorio
 * (ni por RUT ni por patente) se agrega; una persona existente nunca se sobrescribe.
 */
class RegisterVehicleEntryUseCase(
    private val validator: ValidateRegistroUseCase,
    private val entries: VehicleEntryRepository,
    private val personas: PersonaRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(shiftId: Long, input: VehicleEntryInput): RegisterEntryOutcome {
        val errors = validator.validateVehicle(input)
        if (errors.isNotEmpty()) return RegisterEntryOutcome.Invalid(errors)
        if (entries.isInside(shiftId, Patente.normalize(input.patente))) {
            return RegisterEntryOutcome.Invalid(listOf(ValidationError(ValidationError.Field.PATENTE, ValidationError.Reason.ALREADY_INSIDE)))
        }

        val registro = RegistroIngreso(
            shiftId = shiftId,
            patente = Patente.normalize(input.patente),
            nombreApellido = input.nombre.trim(),
            rut = Rut.normalizeOrNull(input.rut),
            departamento = input.departamento.trim(),
            descripcionAuto = input.descripcionAuto.trim(),
            empresa = input.empresa.cleanOrNull(),
            tipoPersona = input.tipoPersona.name,
            observaciones = input.observaciones.cleanOrNull(),
            padron = if (input.esPadron) input.padron.cleanOrNull() else null,
            esPadron = input.esPadron,
            entradaAt = clock.now()
        )
        val id = entries.insert(registro)
        addToDirectoryIfUnknown(registro)
        return RegisterEntryOutcome.Saved(id)
    }

    private suspend fun addToDirectoryIfUnknown(r: RegistroIngreso) {
        val known = r.rut?.let { personas.getByRut(it) } ?: personas.getByPatente(r.patente)
        if (known != null) return
        personas.insertIfAbsent(
            Persona(
                patente = r.patente,
                nombreApellido = r.nombreApellido,
                departamento = r.departamento,
                descripcionAuto = r.descripcionAuto,
                empresa = r.empresa,
                rut = r.rut,
                tipoPersona = r.tipoPersona,
                padron = r.padron
            )
        )
    }
}

/** Valida, normaliza y guarda un ingreso al bicicletero. Rechaza un RUT que ya tiene bicicleta dentro. */
class RegisterBikeEntryUseCase(
    private val validator: ValidateRegistroUseCase,
    private val entries: BikeEntryRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(shiftId: Long, input: BikeEntryInput): RegisterEntryOutcome {
        val errors = validator.validateBike(input)
        if (errors.isNotEmpty()) return RegisterEntryOutcome.Invalid(errors)
        if (entries.isInside(shiftId, Rut.normalize(input.rut))) {
            return RegisterEntryOutcome.Invalid(listOf(ValidationError(ValidationError.Field.RUT, ValidationError.Reason.ALREADY_INSIDE)))
        }

        val id = entries.insert(
            RegistroBicicleta(
                shiftId = shiftId,
                rut = Rut.normalize(input.rut),
                nombre = input.nombre.trim(),
                carrera = input.carrera.trim(),
                tipoPersona = input.tipoPersona.name,
                observacion = input.observacion.cleanOrNull(),
                entradaAt = clock.now()
            )
        )
        return RegisterEntryOutcome.Saved(id)
    }
}

/**
 * Registra salidas. Devuelve el instante marcado (para poder deshacer) o `null`
 * si el registro ya tenía salida.
 */
class MarkExitUseCase(
    private val vehicles: VehicleEntryRepository,
    private val bikes: BikeEntryRepository,
    private val clock: Clock
) {
    suspend fun vehicle(id: Long): Long? = clock.now().takeIf { vehicles.markExit(id, it) }
    suspend fun undoVehicle(id: Long, at: Long): Boolean = vehicles.undoExit(id, at)
    suspend fun bike(id: Long): Long? = clock.now().takeIf { bikes.markExit(id, it) }
    suspend fun undoBike(id: Long, at: Long): Boolean = bikes.undoExit(id, at)
}

/** Búsquedas para autocompletar formularios. */
class FindPersonaUseCase(
    private val personas: PersonaRepository,
    private val bikes: BikeEntryRepository
) {
    data class BikeAutofill(val nombre: String, val carrera: String, val tipoPersona: PersonaType)

    suspend fun byPatente(raw: String): Persona? {
        val patente = Patente.normalize(raw)
        if (patente.length < MIN_PLATE_LOOKUP) return null
        return personas.getByPatente(patente)
    }

    suspend fun byRut(raw: String): Persona? {
        if (!Rut.isValid(raw)) return null
        return personas.getByRut(Rut.normalize(raw))
    }

    /** Prioriza el último registro de bicicleta (tiene carrera); si no, usa el directorio. */
    suspend fun bikeAutofill(raw: String): BikeAutofill? {
        if (!Rut.isValid(raw)) return null
        val rut = Rut.normalize(raw)
        bikes.latestByRut(rut)?.let {
            return BikeAutofill(it.nombre, it.carrera, PersonaType.fromCode(it.tipoPersona))
        }
        return personas.getByRut(rut)?.let {
            BikeAutofill(it.nombreApellido, it.departamento, PersonaType.fromCode(it.tipoPersona))
        }
    }

    private companion object {
        const val MIN_PLATE_LOOKUP = 4
    }
}

private fun String?.cleanOrNull(): String? = this?.trim()?.ifEmpty { null }
