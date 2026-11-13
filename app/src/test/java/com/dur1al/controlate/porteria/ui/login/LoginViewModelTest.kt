package com.dur1al.controlate.porteria.ui.login

import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.data.local.security.AdminPinManager
import com.dur1al.controlate.porteria.data.local.security.InMemoryKeyValueStore
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.ShiftType
import com.dur1al.controlate.porteria.domain.usecase.GetActiveShiftUseCase
import com.dur1al.controlate.porteria.domain.usecase.StartShiftUseCase
import com.dur1al.controlate.porteria.testing.FakeClock
import com.dur1al.controlate.porteria.testing.FakeSessionStore
import com.dur1al.controlate.porteria.testing.FakeShiftRepository
import com.dur1al.controlate.porteria.testing.MainDispatcherRule
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

    private fun viewModel() = LoginViewModel(
        getActiveShift = GetActiveShiftUseCase(shifts, session),
        startShift = StartShiftUseCase(shifts, session, clock),
        pins = pins,
        cpuDispatcher = main.dispatcher
    )

    @Test
    fun `without an active shift the form is shown with porteria options`() = runTest {
        val state = viewModel().state.value
        assertFalse(state.checkingSession)
        assertEquals(ControlPoint.ofKind(ControlPoint.Kind.PORTERIA), state.points)
        assertEquals(ControlPoint.PORTERIA_2, state.selectedPoint)
    }

    @Test
    fun `an open shift resumes directly in its control point`() = runTest {
        val started = StartShiftUseCase(shifts, session, clock)("Guardia", ControlPoint.BICICLETERO_2, ShiftType.DAY)
        assertTrue(started is StartShiftUseCase.Outcome.Started)

        val vm = viewModel()

        assertEquals(LoginViewModel.Event.OpenShift(ControlPoint.BICICLETERO_2), vm.events.first())
    }

    @Test
    fun `switching to bicicletero lists bike points and selects the first`() = runTest {
        val vm = viewModel()
        vm.selectKind(ControlPoint.Kind.BICICLETERO)
        assertEquals(ControlPoint.ofKind(ControlPoint.Kind.BICICLETERO), vm.state.value.points)
        assertEquals(ControlPoint.BICICLETERO_1, vm.state.value.selectedPoint)
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
        vm.selectPoint(ControlPoint.PORTERIA_4)
        vm.selectShiftType(ShiftType.NIGHT)
        vm.startShift("Guardia Demo")

        assertEquals(LoginViewModel.Event.OpenShift(ControlPoint.PORTERIA_4), vm.events.first())
        val turno = shifts.get(session.activeShiftId()!!)!!
        assertEquals("NIGHT", turno.tipo)
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
}
