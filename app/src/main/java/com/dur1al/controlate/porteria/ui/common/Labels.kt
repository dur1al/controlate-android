package com.dur1al.controlate.porteria.ui.common

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.model.ShiftType
import com.dur1al.controlate.porteria.domain.model.ValidationError
import com.dur1al.controlate.porteria.domain.report.ReportLabels

/*
 * Traducción de códigos de dominio a recursos Android. Centralizado aquí para que el dominio
 * no dependa de `R` y la UI no repita `when` por todas partes.
 */

/**
 * Color de identidad del punto: se toma de la paleta de su tipo según [ControlPoint.order]
 * (los puntos de fábrica conservan sus colores; los nuevos rotan por la paleta).
 */
@ColorInt
fun ControlPoint.color(context: Context): Int {
    val palette = context.resources.obtainTypedArray(
        if (kind == ControlPoint.Kind.BICICLETERO) R.array.point_palette_bicicletero else R.array.point_palette_porteria
    )
    return try {
        palette.getColor(order.mod(palette.length()), 0)
    } finally {
        palette.recycle()
    }
}

@get:StringRes
val ControlPoint.Kind.labelRes: Int
    get() = when (this) {
        ControlPoint.Kind.PORTERIA -> R.string.control_point_kind_porteria
        ControlPoint.Kind.BICICLETERO -> R.string.control_point_kind_bicicletero
    }

@get:StringRes
val PersonaType.labelRes: Int
    get() = when (this) {
        PersonaType.VISITA -> R.string.persona_type_visita
        PersonaType.ESTUDIANTE -> R.string.persona_type_estudiante
        PersonaType.FUNCIONARIO -> R.string.persona_type_funcionario
        PersonaType.PROVEEDOR -> R.string.persona_type_proveedor
        PersonaType.OTRO -> R.string.persona_type_otro
    }

@get:StringRes
val ShiftType.labelRes: Int
    get() = when (this) {
        ShiftType.DAY -> R.string.shift_type_day
        ShiftType.NIGHT -> R.string.shift_type_night
    }

@get:StringRes
val ValidationError.messageRes: Int
    get() = when (field) {
        ValidationError.Field.PATENTE -> when (reason) {
            ValidationError.Reason.REQUIRED -> R.string.error_plate_required
            ValidationError.Reason.ALREADY_INSIDE -> R.string.error_plate_already_inside
            ValidationError.Reason.DUPLICATE -> R.string.directory_duplicate_plate
            else -> R.string.error_plate_format
        }
        ValidationError.Field.NOMBRE -> when (reason) {
            ValidationError.Reason.TOO_SHORT -> R.string.error_name_too_short
            ValidationError.Reason.TOO_LONG -> R.string.error_name_too_long
            ValidationError.Reason.INVALID_FORMAT -> R.string.error_name_invalid_chars
            else -> R.string.error_name_required
        }
        ValidationError.Field.RUT -> when (reason) {
            ValidationError.Reason.REQUIRED -> R.string.error_rut_required
            ValidationError.Reason.ALREADY_INSIDE -> R.string.error_rut_already_inside
            else -> R.string.error_rut_invalid
        }
        ValidationError.Field.PADRON -> R.string.error_padron_required
        ValidationError.Field.CARRERA ->
            if (reason == ValidationError.Reason.TOO_LONG) R.string.error_text_too_long else R.string.error_career_required
    }

/** Nombre de [id] según los puntos cargados; si ya no existe, un texto de respaldo con el id. */
fun Context.controlPointLabel(id: String, points: Map<String, ControlPoint>): String =
    points[id.trim().lowercase()]?.name ?: getString(R.string.unknown_control_point, id)

/** Índice por id (sin distinguir mayúsculas) para resolver nombres en listas. */
fun List<ControlPoint>.byId(): Map<String, ControlPoint> = associateBy { it.id.lowercase() }

/** [ReportLabels] respaldado por `strings.xml`. */
class AndroidReportLabels(private val context: Context) : ReportLabels {
    override fun unknownControlPoint(id: String) = context.getString(R.string.unknown_control_point, id)
    override fun personaType(code: String) = context.getString(PersonaType.fromCode(code).labelRes)
    override fun shiftType(code: String) = context.getString(ShiftType.fromCode(code).labelRes)
}
