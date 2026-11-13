package com.dur1al.controlate.porteria.domain.report

/**
 * Contenido de un reporte, independiente del formato de salida (hoy PDF).
 *
 * El dominio decide qué columnas y filas existen; la capa de datos decide cómo dibujarlas
 * y resuelve títulos y encabezados desde `strings.xml`.
 */
data class ReportDocument(
    val type: Type,
    val meta: List<Meta>,
    val columns: List<Column>,
    val rows: List<List<String>>,
    /** Nombre de archivo sin extensión, ya saneado. */
    val fileStem: String
) {
    enum class Type { VEHICLE_SHIFT, BIKE_SHIFT, DAILY_SUMMARY, DIRECTORY }

    data class Meta(val field: MetaField, val value: String)

    enum class MetaField { POINT, DATE, GUARD, SHIFT_TYPE }

    /** [weight] define el ancho relativo de la columna en la tabla. */
    enum class Column(val weight: Float) {
        INDEX(0.5f), PATENTE(1.2f), NOMBRE(2.6f), RUT(1.7f), DEPTO(1.3f), TIPO(1.4f),
        CARRERA(2.0f), ENTRADA(0.9f), SALIDA(0.9f),
        PUNTO(3.0f), ENTRARON(1.2f), SALIERON(1.2f), DENTRO(1.2f)
    }

    companion object {
        /** Marcador de la fila de totales; el renderizador lo reemplaza por el texto localizado. */
        const val TOTAL_ROW_LABEL = "\u0000TOTAL"
        const val NO_TIME = "--:--"
        const val EMPTY = "-"
    }
}

/** Resuelve etiquetas visibles de códigos de dominio (implementado en la capa de presentación). */
interface ReportLabels {
    fun controlPoint(id: String): String
    fun personaType(code: String): String
    fun shiftType(code: String): String
}

/** Persiste un [ReportDocument] en algún formato (PDF en producción) y devuelve el archivo. */
fun interface ReportExporter {
    suspend fun export(document: ReportDocument): java.io.File
}
