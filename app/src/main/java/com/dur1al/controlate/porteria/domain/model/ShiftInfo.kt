package com.dur1al.controlate.porteria.domain.model

/**
 * Información del turno activo, desacoplada de [SessionManager].
 * Permite pasar contexto de turno a use cases sin depender de SharedPreferences.
 */
data class ShiftInfo(
    val guardiaName: String,
    val porteriaId: String,
    val porteriaName: String,
    val fechaTurno: String,
    val turnoTipo: String,
    val shiftId: Long
)
