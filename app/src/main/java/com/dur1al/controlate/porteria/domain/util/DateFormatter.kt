package com.dur1al.controlate.porteria.domain.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centraliza todos los formatos de fecha/hora usados en la aplicación.
 * Thread-safe mediante ThreadLocal para SimpleDateFormat.
 */
object DateFormatter {

    // region Format patterns

    /** Formato para almacenar fechas en la BD (porterías). */
    private val DB_DATE = threadLocalFormat("yyyy-MM-dd")

    /** Formato para mostrar fechas al usuario (chileno). */
    private val DISPLAY_DATE = threadLocalFormat("dd/MM/yyyy")

    /** Formato para hora (24h). */
    private val TIME_24H = threadLocalFormat("HH:mm")

    /** Formato para timestamps de archivos. */
    private val FILE_TIMESTAMP = threadLocalFormat("yyyyMMdd_HHmmss")

    /** Formato compacto para reportes. */
    private val REPORT_TIMESTAMP = threadLocalFormat("ddMMyyyy_HHmm")

    /** Formato para display con hora. */
    private val DISPLAY_DATETIME = threadLocalFormat("dd/MM/yyyy HH:mm")

    // endregion

    // region Public API

    /** Fecha actual en formato BD: `2026-06-14`. */
    fun todayDbFormat(): String = DB_DATE.get()!!.format(Date())

    /** Fecha actual en formato display: `14/06/2026`. */
    fun todayDisplayFormat(): String = DISPLAY_DATE.get()!!.format(Date())

    /** Hora actual en formato 24h: `22:48`. */
    fun nowTime(): String = TIME_24H.get()!!.format(Date())

    /** Timestamp para nombres de archivo: `20260614_224834`. */
    fun fileTimestamp(): String = FILE_TIMESTAMP.get()!!.format(Date())

    /** Timestamp compacto para reportes: `14062026_2248`. */
    fun reportTimestamp(): String = REPORT_TIMESTAMP.get()!!.format(Date())

    /** Fecha y hora formateada para display: `14/06/2026 22:48`. */
    fun nowDisplayDateTime(): String = DISPLAY_DATETIME.get()!!.format(Date())

    /** Convierte fecha DB (`yyyy-MM-dd`) a display (`dd/MM/yyyy`). */
    fun dbToDisplay(dbDate: String): String {
        return try {
            val date = DB_DATE.get()!!.parse(dbDate) ?: return dbDate
            DISPLAY_DATE.get()!!.format(date)
        } catch (_: Exception) {
            dbDate
        }
    }

    /** Convierte fecha display (`dd/MM/yyyy`) a DB (`yyyy-MM-dd`). */
    fun displayToDb(displayDate: String): String {
        return try {
            val date = DISPLAY_DATE.get()!!.parse(displayDate) ?: return displayDate
            DB_DATE.get()!!.format(date)
        } catch (_: Exception) {
            displayDate
        }
    }

    /** Parsea una fecha en cualquier formato reconocido. */
    fun parseFlexible(value: String): Date? {
        val patterns = listOf("yyyy-MM-dd", "dd/MM/yyyy", "yyyy/MM/dd", "dd-MM-yyyy")
        for (pattern in patterns) {
            val formatter = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                isLenient = false
            }
            val parsed = runCatching { formatter.parse(value) }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    /** Normaliza una fecha flexible a formato DB (`yyyy-MM-dd`). */
    fun normalizeToDbFormat(value: String): String? {
        val date = parseFlexible(value) ?: return null
        return DB_DATE.get()!!.format(date)
    }

    // endregion

    private fun threadLocalFormat(pattern: String): ThreadLocal<SimpleDateFormat> {
        return object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat(pattern, Locale.getDefault())
            }
        }
    }
}
