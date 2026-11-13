package com.dur1al.controlate.porteria.domain.importer

import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.validation.Patente
import com.dur1al.controlate.porteria.domain.validation.Rut

/**
 * Parser del formato de importación masiva del directorio:
 * `PATENTE, NOMBRE, DEPTO, RUT, PADRON, EMPRESA, VEHICULO, TIPO` (solo los dos primeros obligatorios).
 *
 * Es puro y determinista para poder probarlo sin Android. Si una patente se repite en el
 * archivo, gana la última aparición (misma semántica que el `REPLACE` de la base).
 */
object PersonaCsvParser {

    data class Result(val personas: List<Persona>, val errors: List<LineError>)

    data class LineError(val line: Int, val reason: Reason) {
        enum class Reason { MISSING_FIELDS, INVALID_PLATE, MISSING_NAME, INVALID_RUT }
    }

    private const val DEFAULT_PLACEHOLDER = "-"

    fun parse(content: String): Result {
        val byPlate = LinkedHashMap<String, Persona>()
        val errors = mutableListOf<LineError>()

        content.lines().forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            if (rawLine.isBlank()) return@forEachIndexed
            val cols = rawLine.split(',').map { it.trim() }
            if (index == 0 && cols.first().equals("PATENTE", ignoreCase = true)) return@forEachIndexed

            when (val parsed = parseLine(cols)) {
                is LineOutcome.Ok -> {
                    byPlate.remove(parsed.persona.patente)
                    byPlate[parsed.persona.patente] = parsed.persona
                }
                is LineOutcome.Failed -> errors += LineError(lineNumber, parsed.reason)
            }
        }
        return Result(byPlate.values.toList(), errors)
    }

    private sealed interface LineOutcome {
        data class Ok(val persona: Persona) : LineOutcome
        data class Failed(val reason: LineError.Reason) : LineOutcome
    }

    private fun parseLine(cols: List<String>): LineOutcome {
        if (cols.size < 2) return LineOutcome.Failed(LineError.Reason.MISSING_FIELDS)
        val patente = Patente.normalize(cols[0])
        if (!Patente.isValid(patente)) return LineOutcome.Failed(LineError.Reason.INVALID_PLATE)
        val nombre = cols[1]
        if (nombre.isBlank()) return LineOutcome.Failed(LineError.Reason.MISSING_NAME)
        // Se valida el texto tal como viene: normalizar antes ocultaría una K o letras en el cuerpo.
        val rawRut = cols.getOrNull(3)?.takeIf { it.isNotBlank() }
        if (rawRut != null && !Rut.isValid(rawRut)) return LineOutcome.Failed(LineError.Reason.INVALID_RUT)
        val rut = rawRut?.let(Rut::normalize)

        return LineOutcome.Ok(
            Persona(
                patente = patente,
                nombreApellido = nombre,
                departamento = cols.getOrNull(2).orPlaceholder(),
                rut = rut,
                padron = cols.getOrNull(4)?.ifBlank { null },
                empresa = cols.getOrNull(5)?.ifBlank { null },
                descripcionAuto = cols.getOrNull(6).orPlaceholder(),
                tipoPersona = cols.getOrNull(7)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { PersonaType.fromCode(it).name }
                    ?: PersonaType.FUNCIONARIO.name
            )
        )
    }

    private fun String?.orPlaceholder(): String = this?.ifBlank { null } ?: DEFAULT_PLACEHOLDER
}
