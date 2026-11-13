package com.dur1al.controlate.porteria.ui.common

import android.content.Context
import androidx.annotation.ColorRes
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

@get:StringRes
val ControlPoint.labelRes: Int
    get() = when (this) {
        ControlPoint.PORTERIA_2 -> R.string.porteria_2
        ControlPoint.PORTERIA_3 -> R.string.porteria_3
        ControlPoint.PORTERIA_4 -> R.string.porteria_4
        ControlPoint.BICICLETERO_1 -> R.string.bicicletero_1
        ControlPoint.BICICLETERO_2 -> R.string.bicicletero_2
    }

@get:ColorRes
val ControlPoint.colorRes: Int
    get() = when (this) {
        ControlPoint.PORTERIA_2 -> R.color.point_porteria_2
        ControlPoint.PORTERIA_3 -> R.color.point_porteria_3
        ControlPoint.PORTERIA_4 -> R.color.point_porteria_4
        ControlPoint.BICICLETERO_1 -> R.color.point_bicicletero_1
        ControlPoint.BICICLETERO_2 -> R.color.point_bicicletero_2
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
        ValidationError.Field.PATENTE -> R.string.error_plate_format
        ValidationError.Field.NOMBRE ->
            if (reason == ValidationError.Reason.TOO_SHORT) R.string.error_name_too_short else R.string.error_name_required
        ValidationError.Field.RUT ->
            if (reason == ValidationError.Reason.REQUIRED) R.string.error_rut_required else R.string.error_rut_invalid
        ValidationError.Field.PADRON -> R.string.error_padron_required
        ValidationError.Field.CARRERA -> R.string.error_career_required
    }

fun Context.controlPointLabel(id: String): String =
    ControlPoint.fromId(id)?.let { getString(it.labelRes) } ?: getString(R.string.unknown_control_point, id)

/** [ReportLabels] respaldado por `strings.xml`. */
class AndroidReportLabels(private val context: Context) : ReportLabels {
    override fun controlPoint(id: String) = context.controlPointLabel(id)
    override fun personaType(code: String) = context.getString(PersonaType.fromCode(code).labelRes)
    override fun shiftType(code: String) = context.getString(ShiftType.fromCode(code).labelRes)
}
