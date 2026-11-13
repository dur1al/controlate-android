package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.data.local.entities.Turno
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.ShiftType
import com.dur1al.controlate.porteria.testing.FakeClock
import com.dur1al.controlate.porteria.testing.FakeSessionStore
import com.dur1al.controlate.porteria.testing.FakeShiftRepository
import com.dur1al.controlate.porteria.testing.Points
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class ShiftUseCasesTest {

    private val zone = ZoneId.of("America/Santiago")
    private val clock = FakeClock()
    private val session = FakeSessionStore()
    private val shifts = FakeShiftRepository()

    private val start = StartShiftUseCase(shifts, session, clock, zone)
    private val getActive = GetActiveShiftUseCase(shifts, session)
    private val end = EndShiftUseCase(shifts, session, clock)

    @Test
    fun `start persists the shift and points the session to it`() = runTest {
        val outcome = start("  Guardia Demo ", Points.PORTERIA_2, ShiftType.NIGHT)

        val turno = (outcome as StartShiftUseCase.Outcome.Started).turno
        assertEquals("Guardia Demo", turno.guardia)
        assertEquals("porteria_3", turno.controlPoint)
        assertEquals("NIGHT", turno.tipo)
        assertEquals(clock.nowMs, turno.iniciadoAt)
        assertNull(turno.finalizadoAt)
        assertEquals(turno.id, session.activeShiftId())
        assertEquals(turno, shifts.get(turno.id))
    }

    @Test
    fun `start rejects guard names shorter than three characters`() = runTest {
        assertEquals(StartShiftUseCase.Outcome.InvalidGuardName, start("Al", Points.PORTERIA_1, ShiftType.DAY))
        assertNull(session.activeShiftId())
    }

    @Test
    fun `active shift is returned while open`() = runTest {
        val started = (start("Guardia", Points.BICICLETERO_1, ShiftType.DAY) as StartShiftUseCase.Outcome.Started).turno
        assertEquals(started, getActive())
    }

    @Test
    fun `stale session pointing to a closed or missing shift is cleared`() = runTest {
        session.setActiveShift(99)
        assertNull(getActive())
        assertNull(session.activeShiftId())

        val id = shifts.insert(Turno(guardia = "G", controlPoint = "porteria_2", tipo = "DAY", fecha = "2026-06-14", iniciadoAt = 1, finalizadoAt = 2))
        session.setActiveShift(id)
        assertNull(getActive())
        assertNull(session.activeShiftId())
    }

    @Test
    fun `end closes the shift at the current time and clears the session`() = runTest {
        val started = (start("Guardia", Points.PORTERIA_1, ShiftType.DAY) as StartShiftUseCase.Outcome.Started).turno
        clock.advanceMinutes(90)

        val closed = end()

        assertEquals(started.id, closed?.id)
        assertEquals(clock.nowMs, shifts.get(started.id)?.finalizadoAt)
        assertNull(session.activeShiftId())
    }

    @Test
    fun `end without an active shift is a no-op`() = runTest {
        assertNull(end())
        assertTrue(shifts.turnos.value.isEmpty())
    }
}
