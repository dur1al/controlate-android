package com.dur1al.controlate

import com.dur1al.controlate.porteria.domain.model.Result
import com.dur1al.controlate.porteria.domain.usecase.ValidateRegistroUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateRegistroUseCaseTest {

    private val useCase = ValidateRegistroUseCase()

    @Test
    fun `validatePorteriaEntry returns empty errors for valid entry`() {
        val result = useCase.validatePorteriaEntry(
            patente = "DEMO01",
            nombre = "Ana Torres",
            rut = "11.111.111-1",
            esPadron = false,
            padron = null
        )

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun `validatePorteriaEntry flags invalid plate, blank name and missing padron`() {
        val result = useCase.validatePorteriaEntry(
            patente = "A-1",
            nombre = "",
            rut = null,
            esPadron = true,
            padron = ""
        )

        assertTrue(result is Result.Success)
        val fields = (result as Result.Success).data.map { it.field }.toSet()
        assertEquals(
            setOf(
                ValidateRegistroUseCase.Field.PATENTE,
                ValidateRegistroUseCase.Field.NOMBRE,
                ValidateRegistroUseCase.Field.PADRON
            ),
            fields
        )
    }

    @Test
    fun `validateBicicletaEntry requires valid rut, name and career`() {
        val result = useCase.validateBicicletaEntry(
            rut = "11.111.111-2",
            nombre = "",
            carrera = ""
        )

        assertTrue(result is Result.Success)
        val fields = (result as Result.Success).data.map { it.field }.toSet()
        assertEquals(
            setOf(
                ValidateRegistroUseCase.Field.RUT,
                ValidateRegistroUseCase.Field.NOMBRE,
                ValidateRegistroUseCase.Field.CARRERA
            ),
            fields
        )
    }
}
