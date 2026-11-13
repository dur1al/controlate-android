package com.dur1al.controlate.porteria.ui.login

import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.local.security.AdminPinManager
import com.dur1al.controlate.porteria.data.local.security.InMemoryKeyValueStore
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.ShiftType
import com.dur1al.controlate.porteria.domain.usecase.GetActiveShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.StartShiftUseCase
import com.dur1al.controlate.porteria.testing.FakeClock
import com.dur1al.controlate.porteria.testing.FakeControlPointRepository
import com.dur1al.controlate.porteria.testing.FakeSessionStore
import com.dur1al.controlate.porteria.testing.FakeShiftRepository
import com.dur1al.controlate.porteria.testing.MainDispatcherRule
import com.dur1al.controlate.porteria.testing.Points
import com.dur1al.controlate.porteria.ui.common.UiMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val clock = FakeClock()
    private val session = FakeSessionStore()
    private val shifts = FakeShiftRepository()
    private val pins = AdminPinManager(InMemoryKeyValueStore(), clock)
    private val points = FakeControlPointRepository()

    private fun viewModel() = LoginViewModel(
        getActiveShift = GetActiveShiftUseCase(shifts, session),
        startShift = StartShiftUseCase(shifts, session, clock),
        pins = pins,
        controlPoints = points,
        cpuDispatcher = main.dispatcher
    )

    @Test
    fun `without an active shift the form is shown with porteria options`() = runTest {
        val state = viewModel().state.value
        assertFalse(state.checkingSession)
        assertEquals(listOf(Points.PORTERIA_1, Points.PORTERIA_2, Points.PORTERIA_3), state.points)
        assertEquals(Points.PORTERIA_1, state.selectedPoint)
    }

    @Test
    fun `an open shift resumes directly in its control point`() = runTest {
        val started = StartShiftUseCase(shifts, session, clock)("Guardia", Points.BICICLETERO_2, ShiftType.DAY)
        assertTrue(started is StartShiftUseCase.Outcome.Started)

        val vm = viewModel()

        assertEquals(LoginViewModel.Event.OpenShift(ControlPoint.Kind.BICICLETERO), vm.events.first())
    }

    @Test
    fun `switching to bicicletero lists bike points and selects the first`() = runTest {
        val vm = viewModel()
        vm.selectKind(ControlPoint.Kind.BICICLETERO)
        assertEquals(listOf(Points.BICICLETERO_1, Points.BICICLETERO_2), vm.state.value.points)
        assertEquals(Points.BICICLETERO_1, vm.state.value.selectedPoint)
    }

    @Test
    fun `short guard name flags the field and does not start a shift`() = runTest {
        val vm = viewModel()
        vm.startShift("Al")
        assertTrue(vm.state.value.guardNameError)
        assertEquals(null, session.activeShiftId())
    }

    @Test
    fun `valid form starts the shift and opens the dashboard`() = runTest {
        val vm = viewModel()
        vm.selectPoint(Points.PORTERIA_3)
        vm.selectShiftType(ShiftType.NIGHT)
        vm.startShift("Guardia Demo")

        assertEquals(LoginViewModel.Event.OpenShift(ControlPoint.Kind.PORTERIA), vm.events.first())
        val turno = shifts.get(session.activeShiftId()!!)!!
        assertEquals("NIGHT", turno.tipo)
        assertEquals(Points.PORTERIA_3.id, turno.controlPoint)
    }

    @Test
    fun `admin access asks for setup on first use, then for the pin`() = runTest {
        val vm = viewModel()
        vm.onAdminClicked()
        assertEquals(LoginViewModel.Event.AskPinSetup, vm.events.first())

        vm.setupPin("4829", "4829")
        assertEquals(LoginViewModel.Event.OpenAdmin, vm.events.first())

        vm.onAdminClicked()
        assertEquals(LoginViewModel.Event.AskPin, vm.events.first())
    }

    @Test
    fun `pin setup reports mismatch and invalid format`() = runTest {
        val vm = viewModel()
        vm.setupPin("4829", "4828")
        assertEquals(LoginViewModel.Event.Message(UiMessage(R.string.login_admin_pin_mismatch)), vm.events.first())
        vm.setupPin("12", "12")
        assertEquals(LoginViewModel.Event.Message(UiMessage(R.string.login_admin_pin_invalid_format)), vm.events.first())
    }

    @Test
    fun `wrong pin reports remaining attempts and right pin opens admin`() = runTest {
        pins.setPin("4829")
        val vm = viewModel()
        vm.submitPin("0000")
        assertEquals(LoginViewModel.Event.Message(UiMessage(R.string.admin_pin_wrong_attempts, listOf(4))), vm.events.first())
        vm.submitPin("4829")
        assertEquals(LoginViewModel.Event.OpenAdmin, vm.events.first())
    }

    @Test
    fun `locked out admin access shows the remaining wait`() = runTest {
        pins.setPin("4829")
        repeat(5) { pins.verify("0000") }
        val vm = viewModel()
        vm.onAdminClicked()
        assertEquals(LoginViewModel.Event.Message(UiMessage(R.string.login_admin_locked_out, listOf(30L))), vm.events.first())
    }

    @Test
    fun `only active points are offered and the list follows admin changes`() = runTest {
        val vm = viewModel()
        points.update(Points.PORTERIA_1.copy(active = false))
        assertEquals(listOf(Points.PORTERIA_2, Points.PORTERIA_3), vm.state.value.points)
        assertEquals(Points.PORTERIA_2, vm.state.value.selectedPoint)

        val nueva = ControlPoint("porteria_5", "Portería 4", ControlPoint.Kind.PORTERIA, 3)
        points.insert(nueva)
        assertEquals(nueva, vm.state.value.points.last())
        assertEquals(Points.PORTERIA_2, vm.state.value.selectedPoint)
    }

    @Test
    fun `a renamed point keeps being selected with its new name`() = runTest {
        val vm = viewModel()
        vm.selectPoint(Points.PORTERIA_2)
        points.update(Points.PORTERIA_2.copy(name = "Acceso Norte"))
        assertEquals("Acceso Norte", vm.state.value.selectedPoint?.name)
    }

    @Test
    fun `without active points of the kind the shift cannot start`() = runTest {
        points.rows.value = points.rows.value.filter { it.kind == ControlPoint.Kind.PORTERIA }
        val vm = viewModel()
        vm.selectKind(ControlPoint.Kind.BICICLETERO)
        assertEquals(null, vm.state.value.selectedPoint)

        vm.startShift("Guardia Demo")

        assertEquals(LoginViewModel.Event.Message(UiMessage(R.string.login_no_active_points)), vm.events.first())
        assertEquals(null, session.activeShiftId())
    }

    @Test
    fun `an open shift whose point no longer exists resumes by its id prefix`() = runTest {
        shifts.insert(com.dur1al.controlate.porteria.data.local.entities.Turno(guardia = "G", controlPoint = "bicicletero_9", tipo = "DAY", fecha = "2026-06-14", iniciadoAt = 1))
            .also(session::setActiveShift)
        assertEquals(LoginViewModel.Event.OpenShift(ControlPoint.Kind.BICICLETERO), viewModel().events.first())
    }
}
