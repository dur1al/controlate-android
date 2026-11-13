package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.repository.ControlPointRepository

/** Motivo por el que un nombre de punto de control no se acepta. */
enum class ControlPointNameError { REQUIRED, TOO_SHORT, TOO_LONG, INVALID_CHARS, DUPLICATE }

/**
 * Reglas del nombre de un punto: 3 a 30 caracteres, letras, dígitos, espacios y `. - # °`, y único
 * entre todos los puntos (también los inactivos, para que los reportes no sean ambiguos).
 * La comparación ignora mayúsculas (no tildes) y espacios repetidos.
 */
class ValidateControlPointNameUseCase {

    /** Nombre tal como se guardará: sin espacios extremos ni repetidos. */
    fun clean(raw: String): String = raw.trim().replace(WHITESPACE, " ")

    operator fun invoke(raw: String, existing: List<ControlPoint>, ignoreId: String? = null): ControlPointNameError? {
        val name = clean(raw)
        return when {
            name.isEmpty() -> ControlPointNameError.REQUIRED
            name.length < MIN_LENGTH -> ControlPointNameError.TOO_SHORT
            name.length > MAX_LENGTH -> ControlPointNameError.TOO_LONG
            !ALLOWED.matches(name) -> ControlPointNameError.INVALID_CHARS
            existing.any { it.id != ignoreId && it.name.equals(name, ignoreCase = true) } -> ControlPointNameError.DUPLICATE
            else -> null
        }
    }

    companion object {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 30
        private val WHITESPACE = Regex("\\s+")
        private val ALLOWED = Regex("^(?=.*[\\p{L}\\d])[\\p{L}\\d .#°-]+$")
    }
}

/** Crea un punto nuevo con id técnico propio (`<tipo>_<n>`), al final de su tipo y activo. */
class AddControlPointUseCase(
    private val points: ControlPointRepository,
    private val validateName: ValidateControlPointNameUseCase
) {
    sealed interface Outcome {
        data class Added(val point: ControlPoint) : Outcome
        data class Invalid(val error: ControlPointNameError) : Outcome
    }

    suspend operator fun invoke(kind: ControlPoint.Kind, rawName: String): Outcome {
        val all = points.getAll()
        validateName(rawName, all)?.let { return Outcome.Invalid(it) }
        val point = ControlPoint(
            id = nextId(kind, all),
            name = validateName.clean(rawName),
            kind = kind,
            order = (all.filter { it.kind == kind }.maxOfOrNull { it.order } ?: -1) + 1
        )
        points.insert(point)
        return Outcome.Added(point)
    }

    /** Siguiente número libre del prefijo; nunca reutiliza el id de un punto existente (aunque esté inactivo). */
    private fun nextId(kind: ControlPoint.Kind, all: List<ControlPoint>): String {
        val prefix = "${kind.idPrefix}_"
        val max = all.mapNotNull { p ->
            p.id.lowercase().takeIf { it.startsWith(prefix) }?.removePrefix(prefix)?.toIntOrNull()
        }.maxOrNull() ?: 0
        return "$prefix${max + 1}"
    }
}

/** Cambia el nombre visible; el id (y por lo tanto el historial) no cambia. */
class RenameControlPointUseCase(
    private val points: ControlPointRepository,
    private val validateName: ValidateControlPointNameUseCase
) {
    sealed interface Outcome {
        data class Renamed(val point: ControlPoint) : Outcome
        data class Invalid(val error: ControlPointNameError) : Outcome
        data object NotFound : Outcome
    }

    suspend operator fun invoke(id: String, rawName: String): Outcome {
        val all = points.getAll()
        val current = all.firstOrNull { it.id == id } ?: return Outcome.NotFound
        validateName(rawName, all, ignoreId = id)?.let { return Outcome.Invalid(it) }
        val renamed = current.copy(name = validateName.clean(rawName))
        points.update(renamed)
        return Outcome.Renamed(renamed)
    }
}

/**
 * Activa o desactiva un punto. No se permite desactivar el último punto activo de un tipo:
 * el guardia quedaría sin opciones para iniciar turno.
 */
class SetControlPointActiveUseCase(private val points: ControlPointRepository) {
    sealed interface Outcome {
        data object Updated : Outcome
        data object LastActiveOfKind : Outcome
        data object NotFound : Outcome
    }

    suspend operator fun invoke(id: String, active: Boolean): Outcome {
        val all = points.getAll()
        val current = all.firstOrNull { it.id == id } ?: return Outcome.NotFound
        if (current.active == active) return Outcome.Updated
        if (!active && all.none { it.id != id && it.kind == current.kind && it.active }) return Outcome.LastActiveOfKind
        points.update(current.copy(active = active))
        return Outcome.Updated
    }
}
