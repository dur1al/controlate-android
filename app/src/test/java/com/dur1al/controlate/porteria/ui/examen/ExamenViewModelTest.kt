package com.dur1al.controlate.porteria.ui.examen

import com.dur1al.controlate.porteria.data.local.entities.ExamenPregunta
import com.dur1al.controlate.porteria.domain.usecase.CalcularResultadoExamenUseCase
import com.dur1al.controlate.porteria.domain.usecase.GenerarExamenUseCase
import com.dur1al.controlate.porteria.testing.FakeClock
import com.dur1al.controlate.porteria.testing.FakeExamRepository
import com.dur1al.controlate.porteria.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExamenViewModelTest {

    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private val repository = FakeExamRepository(
        (1..3).map { ExamenPregunta(id = it, pregunta = "P$it", opcionA = "a", opcionB = "b", opcionC = "c", opcionD = "d", respuestaCorrecta = 1) }
    )

    private fun viewModel(durationSeconds: Long = 60) = ExamenViewModel(
        generar = GenerarExamenUseCase(repository),
        calcular = CalcularResultadoExamenUseCase(repository, FakeClock()),
        guardiaNombre = "Guardia",
        durationSeconds = durationSeconds
    )

    @Test
    fun `loads questions and starts at the first one`() = runTest(main.dispatcher) {
        val vm = viewModel()
        runCurrent()
        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals(3, s.preguntas.size)
        assertEquals(0, s.index)
        assertEquals(60L, s.remainingSeconds)
    }

    @Test
    fun `answers are kept per question while navigating`() = runTest(main.dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.answer(1)
        vm.next()
        vm.answer(3)
        vm.previous()

        assertEquals(0, vm.state.value.index)
        assertEquals(mapOf(0 to 1, 1 to 3), vm.state.value.respuestas)
        assertEquals(1, vm.state.value.selectedOption)
    }

    @Test
    fun `navigation stays within bounds`() = runTest(main.dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.previous()
        assertEquals(0, vm.state.value.index)
        repeat(5) { vm.next() }
        assertEquals(2, vm.state.value.index)
        assertTrue(vm.state.value.isLast)
    }

    @Test
    fun `finishing scores once even if called twice`() = runTest(main.dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.answer(1)
        advanceTimeBy(10_000)
        runCurrent()
        vm.finish()
        vm.finish()
        runCurrent()

        val result = vm.state.value.resultado
        assertNotNull(result)
        assertEquals(1, result!!.puntaje)
        assertEquals(10L, result.tiempoEmpleadoSegundos)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun `timer finishes the exam automatically`() = runTest(main.dispatcher) {
        val vm = viewModel(durationSeconds = 5)
        runCurrent()
        assertNull(vm.state.value.resultado)
        advanceUntilIdle()
        assertEquals(0L, vm.state.value.remainingSeconds)
        assertEquals(5L, vm.state.value.resultado?.tiempoEmpleadoSegundos)
    }
}
