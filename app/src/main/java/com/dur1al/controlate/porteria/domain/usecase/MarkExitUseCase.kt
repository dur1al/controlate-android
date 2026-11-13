package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.data.repository.RegistroRepository
import com.dur1al.controlate.porteria.domain.util.DateFormatter

/**
 * Marca la salida de un registro, estableciendo la hora actual.
 */
class MarkExitUseCase(
    private val registroRepository: RegistroRepository
) {
    suspend operator fun invoke(registro: RegistroIngreso) {
        registroRepository.updateRegistro(
            registro.copy(horaSalida = DateFormatter.nowTime())
        )
    }
}
