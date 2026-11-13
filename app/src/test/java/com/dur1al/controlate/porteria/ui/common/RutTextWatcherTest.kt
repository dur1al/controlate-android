package com.dur1al.controlate.porteria.ui.common

import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** QA del formateo de RUT sobre un `EditText` real: teclas, borrado y texto puesto por código. */
@RunWith(AndroidJUnit4::class)
class RutTextWatcherTest {

    private lateinit var field: EditText

    @Before
    fun setUp() {
        field = EditText(ApplicationProvider.getApplicationContext())
        field.applyRutFormatter()
    }

    private fun type(chars: String) = chars.forEach { field.text.insert(field.selectionEnd, it.toString()) }

    private fun backspace() {
        val end = field.selectionEnd
        field.text.delete(end - 1, end)
    }

    @Test
    fun `typing keeps digit order and cursor at the end`() {
        type("123456785")
        assertEquals("12.345.678-5", field.text.toString())
        assertEquals(field.text.length, field.selectionEnd)
    }

    @Test
    fun `backspace over the dash removes the previous digit`() {
        type("123456785")
        field.setSelection(11)
        backspace()
        assertEquals("1.234.567-5", field.text.toString())
    }

    @Test
    fun `text set by code (edit form) is formatted too`() {
        field.setText("123456785")
        assertEquals("12.345.678-5", field.text.toString())
    }

    @Test
    fun `clearing the field leaves it empty`() {
        type("1234")
        field.setText("")
        assertEquals("", field.text.toString())
    }
}
