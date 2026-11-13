package com.dur1al.controlate.porteria.domain.validation

import com.dur1al.controlate.porteria.domain.validation.RutInputFormatter.Result
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * QA del formateo mientras se escribe. [Field] simula un campo de texto: inserta en el cursor,
 * borra hacia atrás y aplica el formateador igual que lo hace el `TextWatcher`.
 */
class RutInputFormatterTest {

    private class Field(var text: String = "", var cursor: Int = 0) {
        fun type(chars: String) = apply {
            chars.forEach { c ->
                apply(RutInputFormatter.format(text.substring(0, cursor) + c + text.substring(cursor), cursor + 1))
            }
        }

        fun backspace(times: Int = 1) = apply {
            repeat(times) {
                if (cursor == 0) return@repeat
                val deleted = text[cursor - 1]
                val after = text.removeRange(cursor - 1, cursor)
                apply(RutInputFormatter.format(after, cursor - 1, deletedSeparator = !RutInputFormatter.isSignificant(deleted)))
            }
        }

        fun moveTo(position: Int) = apply { cursor = position }

        fun paste(value: String) = apply {
            apply(RutInputFormatter.format(text.substring(0, cursor) + value + text.substring(cursor), cursor + value.length))
        }

        private fun apply(result: Result) {
            text = result.text
            cursor = result.cursor
        }
    }

    @Test
    fun `typing digit by digit keeps the order and the cursor at the end`() {
        val field = Field().type("123456785")
        assertEquals("12.345.678-5", field.text)
        assertEquals(field.text.length, field.cursor)
    }

    @Test
    fun `each keystroke shows the expected intermediate format`() {
        val field = Field()
        val expected = listOf("1", "1-2", "12-3", "123-4", "1.234-5", "12.345-6", "123.456-7", "1.234.567-8", "12.345.678-5")
        "123456785".forEachIndexed { i, c ->
            field.type(c.toString())
            assertEquals("tecla ${i + 1}", expected[i], field.text)
            assertEquals("cursor tras tecla ${i + 1}", field.text.length, field.cursor)
        }
    }

    @Test
    fun `lowercase k is uppercased as check digit`() {
        assertEquals("10.000.013-K", Field().type("10000013k").text)
    }

    @Test
    fun `a K typed in the middle of the body is ignored`() {
        assertEquals("12.345.678-5", Field().type("12K345678-5").text)
    }

    @Test
    fun `dots and dash typed by the guard are accepted and reformatted`() {
        assertEquals("12.345.678-5", Field().type("12.345.678-5").text)
    }

    @Test
    fun `characters beyond nine are ignored`() {
        val field = Field().type("1234567851111")
        assertEquals("12.345.678-5", field.text)
        assertEquals(field.text.length, field.cursor)
    }

    @Test
    fun `backspace over the dash deletes the digit before it`() {
        val field = Field().type("123456785").moveTo(11) // "12.345.678-|5": justo después del guion
        field.backspace()
        assertEquals("1.234.567-5", field.text)
    }

    @Test
    fun `backspace over a dot deletes the digit before it`() {
        val field = Field().type("123456785").moveTo(3) // "12.|345..."
        field.backspace()
        assertEquals("1.345.678-5", field.text)
        assertEquals(1, field.cursor)
    }

    @Test
    fun `backspace from the end erases everything one digit at a time`() {
        val field = Field().type("123456785")
        field.backspace(9)
        assertEquals("", field.text)
        assertEquals(0, field.cursor)
    }

    @Test
    fun `inserting in the middle keeps the cursor after the inserted digit`() {
        val field = Field().type("12345675").moveTo(1) // "1|.234.567-5"
        field.type("9")
        assertEquals("19.234.567-5", field.text)
        assertEquals(2, field.cursor)
    }

    @Test
    fun `pasting a formatted rut with spaces normalizes it`() {
        val field = Field().paste(" 12.345.678-5 ")
        assertEquals("12.345.678-5", field.text)
        assertEquals(field.text.length, field.cursor)
    }

    @Test
    fun `pasting text without rut characters leaves the field empty`() {
        assertEquals("", Field().paste("abc").text)
    }
}
