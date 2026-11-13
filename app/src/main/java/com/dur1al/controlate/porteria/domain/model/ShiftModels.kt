package com.dur1al.controlate.porteria.domain.model

/** Resumen de un turno para el listado de reportes. */
data class ShiftSummary(
    val shiftId: Long,
    val guardia: String,
    val controlPoint: String,
    val tipo: String,
    val fecha: String,
    val iniciadoAt: Long,
    val finalizadoAt: Long?,
    val totalRegistros: Int,
    val totalSalidas: Int
)

/** Conteo de movimientos de un punto de control en un rango de tiempo. */
data class PointStat(val controlPoint: ControlPoint, val entradas: Int, val salidas: Int) {
    /** Ingresos del rango que todavía no registran salida. */
    val dentro: Int get() = (entradas - salidas).coerceAtLeast(0)
}

/** Fila de conteo por punto, tal como la devuelve la consulta agregada de Room. */
data class PointCount(val controlPoint: String, val entradas: Int, val salidas: Int)
