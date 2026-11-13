package com.dur1al.controlate.porteria.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Formato de instantes (epoch millis) para UI y reportes. Los timestamps se persisten como
 * epoch millis; los textos se derivan aquí, en la zona horaria del dispositivo por defecto.
 *
 * `DateTimeFormatter` es inmutable y thread-safe (a diferencia de `SimpleDateFormat`).
 */
object TimeFormat {

    private val TIME = DateTimeFormatter.ofPattern("HH:mm")
    private val ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE
    private val DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")

    fun time(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        TIME.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    fun isoDate(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        ISO_DATE.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    fun displayDate(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        DISPLAY_DATE.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    fun displayDateTime(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        DISPLAY_DATE_TIME.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    fun fileStamp(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        FILE_STAMP.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    /** Convierte `yyyy-MM-dd` a `dd/MM/yyyy`; si no se puede parsear devuelve la entrada intacta. */
    fun isoToDisplay(isoDate: String): String = try {
        DISPLAY_DATE.format(LocalDate.parse(isoDate, ISO_DATE))
    } catch (_: DateTimeParseException) {
        isoDate
    }

    /** Rango inclusivo `[00:00:00.000, 23:59:59.999]` del día local [date]. */
    fun dayRange(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): LongRange {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start..end
    }

    fun today(nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
}

/** Fuente de tiempo inyectable: en producción el reloj del sistema, en tests un valor fijo. */
fun interface Clock {
    fun now(): Long

    companion object {
        val SYSTEM = Clock { System.currentTimeMillis() }
    }
}
