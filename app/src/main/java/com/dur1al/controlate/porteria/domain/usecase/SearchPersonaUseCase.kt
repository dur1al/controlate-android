package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.repository.RegistroRepository

import com.dur1al.controlate.porteria.data.repository.PersonaRepository

/**
 * Busca una persona en el directorio por patente o RUT.
 * Centraliza la lógica de búsqueda que estaba dispersa
 * entre [EntradaBottomSheet] y [DashboardViewModel].
 */
class SearchPersonaUseCase(
    private val repository: PersonaRepository
) {
    /** Busca por patente limpia (uppercase, sin espacios). */
    suspend fun byPatente(patente: String): Persona? {
        val clean = patente.uppercase().replace(" ", "")
        if (clean.length < 4) return null
        return repository.getPersonaByPatente(clean)
    }

    /** Busca por RUT limpio (solo dígitos y K). */
    suspend fun byRut(rut: String): Persona? {
        val clean = rut.replace(Regex("[^0-9kK]"), "").uppercase()
        if (clean.length < 7) return null
        return repository.getPersonaByRut(clean)
    }
}
