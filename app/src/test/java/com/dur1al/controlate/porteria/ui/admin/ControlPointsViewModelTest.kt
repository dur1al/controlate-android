package com.dur1al.controlate.porteria.ui.admin

import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.usecase.AddControlPointUseCase
import com.dur1al.controlate.porteria.domain.usecase.ControlPointNameError
import com.dur1al.controlate.porteria.domain.usecase.RenameControlPointUseCase
import com.dur1al.controlate.porteria.domain.usecase.SetControlPointActiveUseCase
import com.dur1al.controlate.porteria.domain.usecase.ValidateControlPointNameUseCase
import com.dur1al.controlate.porteria.testing.FakeControlPointRepository
import com.dur1al.controlate.porteria.testing.MainDispatcherRule
import com.dur1al.controlate.porteria.testing.Points
import com.dur1al.controlate.porteria.ui.common.UiMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ControlPointsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val points = FakeControlPointRepository()

    private fun viewModel(): ControlPointsViewModel {
        val validate = ValidateControlPointNameUseCase()
        return ControlPointsViewModel(
            repository = points,
            addPoint = AddControlPointUseCase(points, validate),
            renamePoint = RenameControlPointUseCase(points, validate),
            setActive = SetControlPointActiveUseCase(points)
        )
    }

    @Test
    fun `lists every point and suggests the next number per kind`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        assertEquals(ControlPoint.DEFAULTS.size, vm.state.value.points.size)
        assertEquals(4, vm.state.value.nextNumber(ControlPoint.Kind.PORTERIA))
        assertEquals(3, vm.state.value.nextNumber(ControlPoint.Kind.BICICLETERO))
    }

    @Test
    fun `adding a valid point closes the dialog and confirms with its name`() = runTest {
        val vm = viewModel()
        vm.add(ControlPoint.Kind.PORTERIA, "Portería 4")

        assertEquals(ControlPointsViewModel.Event.DialogDone, vm.events.first())
        assertEquals(ControlPointsViewModel.Event.Message(UiMessage(R.string.control_points_added, listOf("Portería 4"))), vm.events.first())
        assertNull(vm.nameError.value)
        assertFalse(vm.saving.value)
    }

    @Test
    fun `an invalid name stays in the dialog as a field error`() = runTest {
        val vm = viewModel()
        vm.add(ControlPoint.Kind.PORTERIA, "P1")
        assertEquals(ControlPointNameError.TOO_SHORT, vm.nameError.value)

        vm.clearNameError()
        vm.rename(Points.PORTERIA_1.id, "Portería 2")
        assertEquals(ControlPointNameError.DUPLICATE, vm.nameError.value)
        assertEquals(ControlPoint.DEFAULTS.size, points.rows.value.size)

        vm.startDialog()
        assertNull(vm.nameError.value)
    }

    @Test
    fun `deactivating the last active point of a kind is explained`() = runTest {
        val vm = viewModel()
        vm.setActive(Points.BICICLETERO_1.id, false)
        assertEquals(ControlPointsViewModel.Event.Message(UiMessage(R.string.control_points_deactivated)), vm.events.first())

        vm.setActive(Points.BICICLETERO_2.id, false)
        assertEquals(ControlPointsViewModel.Event.Message(UiMessage(R.string.control_points_last_active)), vm.events.first())
    }

    @Test
    fun `a storage failure is reported instead of crashing`() = runTest {
        val vm = viewModel()
        points.failWrites = true
        vm.add(ControlPoint.Kind.PORTERIA, "Portería 4")

        // Sin DialogDone: el diálogo queda abierto para reintentar.
        assertEquals(ControlPointsViewModel.Event.Message(UiMessage(R.string.error_generic)), vm.events.first())
        assertFalse(vm.saving.value)
        assertEquals(ControlPoint.DEFAULTS.size, points.rows.value.size)
    }
}
