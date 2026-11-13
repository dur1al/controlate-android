package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.data.repository.PersonaRepository
import com.dur1al.controlate.porteria.data.repository.RegistroRepository
import com.dur1al.controlate.porteria.domain.model.ShiftInfo
import com.dur1al.controlate.porteria.domain.util.DateFormatter

/**
 * Encapsula la lógica de guardar un nuevo registro de ingreso
 * y actualizar el directorio de personas automáticamente.
 */
class SaveRegistroUseCase(
    private val registroRepository: RegistroRepository,
    private val personaRepository: PersonaRepository
) {
    suspend operator fun invoke(
        shiftInfo: ShiftInfo,
        patente: String,
        nombre: String,
        rut: String?,
        padron: String?,
        esPadron: Boolean,
        departamento: String,
        descripcionAuto: String,
        empresa: String?,
        tipoPersona: String?,
        observaciones: String?
    ) {
        val cleanPatente = patente.uppercase().replace(" ", "")
        val registro = RegistroIngreso(
            porteria = shiftInfo.porteriaId,
            guardiaTurno = shiftInfo.guardiaName,
            fechaTurno = shiftInfo.fechaTurno,
            turnoTipo = shiftInfo.turnoTipo,
            shiftId = shiftInfo.shiftId,
            patente = cleanPatente,
            nombreApellido = nombre,
            rut = rut?.ifBlank { null },
            padron = padron?.ifBlank { null },
            esPadron = esPadron,
            departamento = departamento,
            descripcionAuto = descripcionAuto,
            empresa = empresa?.ifBlank { null },
            tipoPersona = tipoPersona,
            observaciones = observaciones?.ifBlank { null },
            horaEntrada = DateFormatter.nowTime(),
            fechaEntrada = DateFormatter.todayDbFormat()
        )
        registroRepository.insertRegistro(registro)
        updatePersonaDirectory(registro)
    }

    private suspend fun updatePersonaDirectory(reg: RegistroIngreso) {
        val existing = if (!reg.rut.isNullOrEmpty()) {
            personaRepository.getPersonaByRut(reg.rut)
        } else null
            ?: personaRepository.getPersonaByPatente(reg.patente)

        if (existing == null) {
            personaRepository.insertAll(
                listOf(
                    Persona(
                        patente = reg.patente,
                        nombreApellido = reg.nombreApellido,
                        rut = reg.rut,
                        padron = reg.padron,
                        departamento = reg.departamento,
                        descripcionAuto = reg.descripcionAuto,
                        tipoPersona = reg.tipoPersona,
                        empresa = reg.empresa
                    )
                )
            )
        }
    }
}
