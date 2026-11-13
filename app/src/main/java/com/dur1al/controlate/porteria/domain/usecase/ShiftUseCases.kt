package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.ShiftType
import com.dur1al.controlate.porteria.domain.repository.SessionStore
import com.dur1al.controlate.porteria.domain.repository.ShiftRepository
import com.dur1al.controlate.porteria.domain.time.Clock
import com.dur1al.controlate.porteria.domain.time.TimeFormat
import java.time.ZoneId

/** Abre un turno: lo persiste y lo marca como activo en este dispositivo. */
class StartShiftUseCase(
    private val shifts: ShiftRepository,
    private val session: SessionStore,
    private val clock: Clock,
    private val zone: ZoneId = ZoneId.systemDefault()
) {
    sealed interface Outcome {
        data class Started(val turno: Turno) : Outcome
        data object InvalidGuardName : Outcome
    }

    suspend operator fun invoke(guardia: String, point: ControlPoint, tipo: ShiftType): Outcome {
        val name = guardia.trim()
        if (name.length < MIN_GUARD_NAME) return Outcome.InvalidGuardName

        val now = clock.now()
        val draft = Turno(
            guardia = name,
            controlPoint = point.id,
            tipo = tipo.name,
            fecha = TimeFormat.isoDate(now, zone),
            iniciadoAt = now
        )
        val turno = draft.copy(id = shifts.insert(draft))
        session.setActiveShift(turno.id)
        return Outcome.Started(turno)
    }

    companion object {
        const val MIN_GUARD_NAME = 3
    }
}

/** Devuelve el turno abierto o `null`; limpia la sesión si apunta a un turno cerrado o inexistente. */
class GetActiveShiftUseCase(
    private val shifts: ShiftRepository,
    private val session: SessionStore
) {
    suspend operator fun invoke(): Turno? {
        val id = session.activeShiftId() ?: return null
        val turno = shifts.get(id)
        if (turno == null || turno.finalizadoAt != null) {
            session.clear()
            return null
        }
        return turno
    }
}

/** Cierra el turno activo con la hora actual. */
class EndShiftUseCase(
    private val shifts: ShiftRepository,
    private val session: SessionStore,
    private val clock: Clock
) {
    suspend operator fun invoke(): Turno? {
        val id = session.activeShiftId() ?: return null
        val now = clock.now()
        shifts.finish(id, now)
        session.clear()
        return shifts.get(id)
    }
}
