package com.dur1al.controlate.porteria.utils

import android.content.Context
import com.dur1al.controlate.porteria.data.ControlPoint

/**
 * Helper para estandarizar la visualización de los puntos de control en toda la app.
 * Esto asegura consistencia entre vistas, reportes y Excel.
 */
object PointDisplayHelper {

    fun getDisplayName(context: Context, pointId: String?): String {
        return context.getString(ControlPoint.fromId(pointId).labelRes)
    }

    fun getPointColor(context: Context, pointId: String?): Int {
        return context.getColor(ControlPoint.fromId(pointId).primaryColorRes)
    }
}
