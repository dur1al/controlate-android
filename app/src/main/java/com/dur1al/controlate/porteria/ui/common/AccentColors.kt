package com.dur1al.controlate.porteria.ui.common

import android.content.res.ColorStateList
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.dur1al.controlate.R
import com.google.android.material.button.MaterialButton
import kotlin.math.pow

/**
 * Contraste WCAG 2.x entre colores ARGB opacos. Lógica pura (sin `android.graphics.Color`)
 * para poder probarla en la JVM.
 */
object Contrast {
    /** Mínimo WCAG AA para texto normal. */
    const val MIN_TEXT_RATIO = 4.5

    fun luminance(@ColorInt color: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((color shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    fun ratio(@ColorInt a: Int, @ColorInt b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** Entre [light] y [dark], el que mejor se lee sobre [background]; ante empate gana [light]. */
    @ColorInt
    fun readableOn(@ColorInt background: Int, @ColorInt light: Int, @ColorInt dark: Int): Int =
        if (ratio(background, light) >= ratio(background, dark)) light else dark
}

/** Texto claro u oscuro de la paleta, el que más contraste tenga sobre [background]. */
@ColorInt
fun TextView.readableColorOn(@ColorInt background: Int): Int = Contrast.readableOn(
    background,
    light = ContextCompat.getColor(context, R.color.white),
    dark = ContextCompat.getColor(context, R.color.black)
)

/**
 * Pinta un botón con un color de acento dinámico (p. ej. el color del punto de control) sin perder
 * legibilidad: el texto y el icono se eligen por contraste, y el estado deshabilitado se distingue
 * con los tokens neutros en vez de verse igual que el activo.
 */
fun MaterialButton.applyAccent(@ColorInt accent: Int) {
    val disabledBg = ContextCompat.getColor(context, R.color.action_disabled_bg)
    val disabledFg = ContextCompat.getColor(context, R.color.text_secondary)
    val foreground = readableColorOn(accent)
    backgroundTintList = enabledStateList(enabled = accent, disabled = disabledBg)
    val content = enabledStateList(enabled = foreground, disabled = disabledFg)
    setTextColor(content)
    iconTint = content
}

/** Icono legible sobre un fondo de color dinámico; al deshabilitarse se atenúa. */
fun ImageButton.applyReadableTint(@ColorInt background: Int) {
    val foreground = Contrast.readableOn(
        background,
        light = ContextCompat.getColor(context, R.color.white),
        dark = ContextCompat.getColor(context, R.color.black)
    )
    imageTintList = enabledStateList(enabled = foreground, disabled = (foreground and 0x00FFFFFF) or DISABLED_ALPHA)
}

private const val DISABLED_ALPHA = 0x80 shl 24

private fun enabledStateList(@ColorInt enabled: Int, @ColorInt disabled: Int) = ColorStateList(
    arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
    intArrayOf(disabled, enabled)
)
