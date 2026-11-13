package com.dur1al.controlate.porteria.domain.usecase

import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.testing.FakeControlPointRepository
import com.dur1al.controlate.porteria.testing.Points
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlPointUseCasesTest {

    private val points = FakeControlPointRepository()
    private val validate = ValidateControlPointNameUseCase()
    private val add = AddControlPointUseCase(points, validate)
    private val rename = RenameControlPointUseCase(points, validate)
    private val setActive = SetControlPointActiveUseCase(points)

    // region Nombre (QA)

    @Test
    fun `name accepts letters, digits, accents and common symbols`() {
        listOf("Portería 4", "Acceso Norte", "Bici #3", "Torre B-2", "Nº 1 Sur", "Puerta 2°", "ÑUÑOA").forEach {
            assertNull("debe aceptar '$it'", validate(it, emptyList()))
        }
    }

    @Test
    fun `name rejects empty, short, long and invalid input`() {
        assertEquals(ControlPointNameError.REQUIRED, validate("   ", emptyList()))
        assertEquals(ControlPointNameError.TOO_SHORT, validate("P1", emptyList()))
        assertEquals(ControlPointNameError.TOO_LONG, validate("x".repeat(31), emptyList()))
        assertNull(validate("x".repeat(30), emptyList()))
        listOf("Portería <1>", "Bici@Sur", "Norte; DROP", "---", "###").forEach {
            assertEquals("debe rechazar '$it'", ControlPointNameError.INVALID_CHARS, validate(it, emptyList()))
        }
    }

    @Test
    fun `name is unique ignoring case and repeated spaces, also against inactive points`() {
        val existing = listOf(Points.PORTERIA_1.copy(active = false))
        assertEquals(ControlPointNameError.DUPLICATE, validate("  portería   1 ", existing))
        assertNull(validate("Portería 1", existing, ignoreId = Points.PORTERIA_1.id))
    }

    @Test
    fun `clean trims and collapses whitespace`() {
        assertEquals("Acceso Norte", validate.clean("  Acceso    Norte  "))
    }

    // endregion

    @Test
    fun `adding a porteria creates the next free id at the end of its kind`() = runTest {
        val outcome = add(ControlPoint.Kind.PORTERIA, "  Portería   4 ") as AddControlPointUseCase.Outcome.Added

        assertEquals(ControlPoint("porteria_5", "Portería 4", ControlPoint.Kind.PORTERIA, 3, active = true), outcome.point)
        assertTrue(points.rows.value.contains(outcome.point))
    }

    @Test
    fun `adding a bicicletero uses its own prefix and order`() = runTest {
        val outcome = add(ControlPoint.Kind.BICICLETERO, "Bici Sur") as AddControlPointUseCase.Outcome.Added
        assertEquals("bicicletero_3", outcome.point.id)
        assertEquals(2, outcome.point.order)
    }

    @Test
    fun `ids are never reused even if the highest point is inactive`() = runTest {
        add(ControlPoint.Kind.PORTERIA, "Portería 4")
        setActive("porteria_5", false)
        val next = add(ControlPoint.Kind.PORTERIA, "Portería 5") as AddControlPointUseCase.Outcome.Added
        assertEquals("porteria_6", next.point.id)
    }

    @Test
    fun `an invalid name is not stored`() = runTest {
        assertEquals(AddControlPointUseCase.Outcome.Invalid(ControlPointNameError.DUPLICATE), add(ControlPoint.Kind.PORTERIA, "PORTERÍA 2"))
        assertEquals(ControlPoint.DEFAULTS.size, points.rows.value.size)
    }

    @Test
    fun `rename keeps id, kind and order`() = runTest {
        val outcome = rename(Points.PORTERIA_2.id, "Acceso Norte") as RenameControlPointUseCase.Outcome.Renamed
        assertEquals(Points.PORTERIA_2.copy(name = "Acceso Norte"), outcome.point)
    }

    @Test
    fun `rename to its own name is allowed and to another point's name is not`() = runTest {
        assertTrue(rename(Points.PORTERIA_2.id, "portería 2") is RenameControlPointUseCase.Outcome.Renamed)
        assertEquals(
            RenameControlPointUseCase.Outcome.Invalid(ControlPointNameError.DUPLICATE),
            rename(Points.PORTERIA_2.id, "Portería 3")
        )
        assertEquals(RenameControlPointUseCase.Outcome.NotFound, rename("nope", "Algo válido"))
    }

    @Test
    fun `the last active point of a kind cannot be deactivated`() = runTest {
        assertEquals(SetControlPointActiveUseCase.Outcome.Updated, setActive(Points.BICICLETERO_1.id, false))
        assertEquals(SetControlPointActiveUseCase.Outcome.LastActiveOfKind, setActive(Points.BICICLETERO_2.id, false))
        assertTrue(points.get(Points.BICICLETERO_2.id)!!.active)

        // Las porterías no cuentan para el límite de los bicicleteros.
        assertEquals(SetControlPointActiveUseCase.Outcome.Updated, setActive(Points.BICICLETERO_1.id, true))
        assertEquals(SetControlPointActiveUseCase.Outcome.NotFound, setActive("nope", true))
    }
}
