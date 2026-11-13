package com.dur1al.controlate.porteria.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val appDark = 0xFF101113.toInt()

    @Test
    fun ratio_matches_wcag_reference_values() {
        assertEquals(21.0, Contrast.ratio(white, black), 0.01)
        assertEquals(1.0, Contrast.ratio(white, white), 0.0)
        // #767676 es el gris más claro que cumple 4.5:1 sobre blanco.
        assertEquals(4.54, Contrast.ratio(0xFF767676.toInt(), white), 0.01)
    }

    @Test
    fun light_accents_get_dark_text() {
        // Cian de "Portería 3" y lila de modo noche: con blanco no se leían.
        assertEquals(appDark, Contrast.readableOn(0xFF00E5FF.toInt(), white, appDark))
        assertEquals(appDark, Contrast.readableOn(0xFFD0BCFF.toInt(), white, appDark))
    }

    @Test
    fun dark_accents_keep_white_text() {
        assertEquals(white, Contrast.readableOn(0xFF7C4DFF.toInt(), white, appDark))
    }

    @Test
    fun action_tokens_with_white_text_meet_wcag_aa() {
        // action_primary, action_bike y action_success llevan texto blanco.
        listOf(0xFF7C4DFF, 0xFF00806A, 0xFF2E7D32).forEach { bg ->
            val ratio = Contrast.ratio(bg.toInt(), white)
            assertTrue("#${bg.toString(16)} = $ratio", ratio >= Contrast.MIN_TEXT_RATIO)
        }
    }

    @Test
    fun disabled_button_text_stays_readable() {
        // text_secondary sobre action_disabled_bg.
        assertTrue(Contrast.ratio(0xFFC7C4D0.toInt(), 0xFF343943.toInt()) >= Contrast.MIN_TEXT_RATIO)
    }
}
