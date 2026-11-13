package com.dur1al.controlate.porteria.data.local.database

import android.database.Cursor
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.model.ShiftType
import com.dur1al.controlate.porteria.domain.validation.Rut
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Turno activo guardado por versiones anteriores solo en SharedPreferences.
 * Se materializa como fila de `turnos` para que la FK de los nuevos registros sea válida.
 */
data class LegacyActiveShift(
    val id: Long,
    val guardia: String,
    val controlPoint: String,
    val turnoTipo: String,
    val fecha: String
)

/**
 * v17 -> v18:
 * - Crea `turnos` y lo puebla desde los datos de turno repetidos en cada registro.
 * - Reemplaza fecha/hora en texto por epoch millis (`entradaAt`, `salidaAt`). Una salida
 *   con hora menor a la entrada se interpreta como del día siguiente (turno noche).
 * - Normaliza RUT a su forma canónica y `tipoPersona` a códigos de [PersonaType].
 * - Elimina preguntas OS-10 duplicadas y agrega índice único por enunciado.
 *
 * La conversión se hace en Kotlin con [zone] explícita (no con `strftime` de SQLite)
 * para que el resultado sea determinista y verificable en tests.
 */
class Migration17To18(
    private val activeShift: LegacyActiveShift?,
    private val zone: ZoneId = ZoneId.systemDefault()
) : Migration(17, 18) {

    override fun migrate(db: SupportSQLiteDatabase) {
        val ingresos = readIngresos(db)
        val bicicletas = readBicicletas(db)
        val turnos = buildTurnos(ingresos, bicicletas)

        createTurnos(db, turnos)
        rebuildIngresos(db, ingresos)
        rebuildBicicletas(db, bicicletas)
        migratePersonas(db)
        migrateExamen(db)
    }

    // region Lectura de filas heredadas

    private data class LegacyIngreso(
        val id: Long, val shiftId: Long, val guardia: String, val point: String, val tipoTurno: String,
        val fechaTurno: String, val patente: String, val nombre: String, val rut: String?,
        val depto: String, val auto: String, val empresa: String?, val tipoPersona: String?,
        val observaciones: String?, val padron: String?, val esPadron: Int,
        val entradaAt: Long, val salidaAt: Long?
    )

    private data class LegacyBicicleta(
        val id: Long, val shiftId: Long, val guardia: String, val point: String, val tipoTurno: String,
        val fecha: String, val rut: String, val nombre: String, val carrera: String,
        val tipoPersona: String, val observacion: String?, val entradaAt: Long, val salidaAt: Long?
    )

    private fun readIngresos(db: SupportSQLiteDatabase): List<LegacyIngreso> =
        db.query("SELECT * FROM registros_ingreso").use { c ->
            buildList {
                while (c.moveToNext()) {
                    val shiftId = c.long("shiftId")
                    val date = parseDate(c.str("fechaEntrada")) ?: parseDate(c.str("fechaTurno"))
                    val (entrada, salida) = toEpochPair(date, c.str("horaEntrada"), c.strOrNull("horaSalida"), shiftId)
                    add(
                        LegacyIngreso(
                            id = c.long("id"), shiftId = shiftId, guardia = c.str("guardiaTurno"),
                            point = c.str("porteria"), tipoTurno = c.str("turnoTipo"), fechaTurno = c.str("fechaTurno"),
                            patente = c.str("patente"), nombre = c.str("nombreApellido"), rut = c.strOrNull("rut"),
                            depto = c.str("departamento"), auto = c.str("descripcionAuto"), empresa = c.strOrNull("empresa"),
                            tipoPersona = c.strOrNull("tipoPersona"), observaciones = c.strOrNull("observaciones"),
                            padron = c.strOrNull("padron"), esPadron = c.getInt(c.getColumnIndexOrThrow("esPadron")),
                            entradaAt = entrada, salidaAt = salida
                        )
                    )
                }
            }
        }

    private fun readBicicletas(db: SupportSQLiteDatabase): List<LegacyBicicleta> =
        db.query("SELECT * FROM registros_bicicleta").use { c ->
            buildList {
                while (c.moveToNext()) {
                    val shiftId = c.long("shiftId")
                    val date = parseDate(c.str("fecha"))
                    val (entrada, salida) = toEpochPair(date, c.str("horaEntrada"), c.strOrNull("horaSalida"), shiftId)
                    add(
                        LegacyBicicleta(
                            id = c.long("id"), shiftId = shiftId, guardia = c.str("guardiaTurno"),
                            point = c.str("controlPoint"), tipoTurno = c.str("turnoTipo"), fecha = c.str("fecha"),
                            rut = c.str("rut"), nombre = c.str("nombre"), carrera = c.str("carrera"),
                            tipoPersona = c.str("tipoPersona"), observacion = c.strOrNull("observacionBicicleta"),
                            entradaAt = entrada, salidaAt = salida
                        )
                    )
                }
            }
        }

    // endregion

    // region Turnos

    private data class TurnoRow(
        val id: Long, val guardia: String, val point: String, val tipo: String,
        val fecha: String, val iniciadoAt: Long, val finalizadoAt: Long?
    )

    private fun buildTurnos(ingresos: List<LegacyIngreso>, bicicletas: List<LegacyBicicleta>): List<TurnoRow> {
        val result = LinkedHashMap<Long, TurnoRow>()

        fun register(
            shiftId: Long, guardia: String, point: String, tipoTurno: String, fecha: String?,
            entradas: List<Long>, cierres: List<Long>, defaultPoint: String
        ) {
            val iniciadoAt = if (shiftId > 0) shiftId else entradas.minOrNull() ?: 0L
            result[shiftId] = TurnoRow(
                id = shiftId,
                guardia = guardia.ifBlank { UNKNOWN_GUARD },
                point = point.trim().lowercase().ifBlank { defaultPoint },
                tipo = ShiftType.fromCode(tipoTurno).name,
                fecha = parseDate(fecha.orEmpty())?.toString() ?: isoDate(iniciadoAt),
                iniciadoAt = iniciadoAt,
                finalizadoAt = (cierres + iniciadoAt).max()
            )
        }

        ingresos.groupBy { it.shiftId }.forEach { (shiftId, rows) ->
            val first = rows.first()
            register(
                shiftId, rows.firstOrNull { it.guardia.isNotBlank() }?.guardia.orEmpty(), first.point,
                first.tipoTurno, first.fechaTurno, rows.map { it.entradaAt },
                rows.map { it.salidaAt ?: it.entradaAt }, DEFAULT_PORTERIA
            )
        }
        bicicletas.groupBy { it.shiftId }.forEach { (shiftId, rows) ->
            if (shiftId in result) return@forEach
            val first = rows.first()
            register(
                shiftId, rows.firstOrNull { it.guardia.isNotBlank() }?.guardia.orEmpty(), first.point,
                first.tipoTurno, first.fecha, rows.map { it.entradaAt },
                rows.map { it.salidaAt ?: it.entradaAt }, DEFAULT_BICICLETERO
            )
        }

        activeShift?.let { active ->
            result[active.id] = TurnoRow(
                id = active.id,
                guardia = active.guardia.ifBlank { result[active.id]?.guardia ?: UNKNOWN_GUARD },
                point = active.controlPoint.trim().lowercase().ifBlank { DEFAULT_PORTERIA },
                tipo = ShiftType.fromCode(active.turnoTipo).name,
                fecha = parseDate(active.fecha)?.toString() ?: isoDate(active.id),
                iniciadoAt = active.id,
                finalizadoAt = null
            )
        }
        return result.values.sortedBy { it.id }
    }

    private fun createTurnos(db: SupportSQLiteDatabase, turnos: List<TurnoRow>) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `turnos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`guardia` TEXT NOT NULL, `controlPoint` TEXT NOT NULL, `tipo` TEXT NOT NULL, `fecha` TEXT NOT NULL, " +
                "`iniciadoAt` INTEGER NOT NULL, `finalizadoAt` INTEGER)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_turnos_iniciadoAt` ON `turnos` (`iniciadoAt`)")
        turnos.forEach { t ->
            db.execSQL(
                "INSERT INTO turnos (id, guardia, controlPoint, tipo, fecha, iniciadoAt, finalizadoAt) VALUES (?,?,?,?,?,?,?)",
                arrayOf(t.id, t.guardia, t.point, t.tipo, t.fecha, t.iniciadoAt, t.finalizadoAt)
            )
        }
    }

    // endregion

    // region Registros

    private fun rebuildIngresos(db: SupportSQLiteDatabase, rows: List<LegacyIngreso>) {
        db.execSQL("DROP TABLE registros_ingreso")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `registros_ingreso` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`shiftId` INTEGER NOT NULL, `patente` TEXT NOT NULL, `nombreApellido` TEXT NOT NULL, `rut` TEXT, " +
                "`departamento` TEXT NOT NULL, `descripcionAuto` TEXT NOT NULL, `empresa` TEXT, `tipoPersona` TEXT NOT NULL, " +
                "`observaciones` TEXT, `padron` TEXT, `esPadron` INTEGER NOT NULL, `entradaAt` INTEGER NOT NULL, " +
                "`salidaAt` INTEGER, FOREIGN KEY(`shiftId`) REFERENCES `turnos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_ingreso_shiftId` ON `registros_ingreso` (`shiftId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_ingreso_patente` ON `registros_ingreso` (`patente`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_ingreso_entradaAt` ON `registros_ingreso` (`entradaAt`)")
        rows.forEach { r ->
            db.execSQL(
                "INSERT INTO registros_ingreso (id, shiftId, patente, nombreApellido, rut, departamento, descripcionAuto, " +
                    "empresa, tipoPersona, observaciones, padron, esPadron, entradaAt, salidaAt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                arrayOf(
                    r.id, r.shiftId, r.patente, r.nombre, Rut.normalizeOrNull(r.rut), r.depto, r.auto, r.empresa,
                    PersonaType.fromCode(r.tipoPersona).name, r.observaciones, r.padron, r.esPadron,
                    r.entradaAt, r.salidaAt
                )
            )
        }
    }

    private fun rebuildBicicletas(db: SupportSQLiteDatabase, rows: List<LegacyBicicleta>) {
        db.execSQL("DROP TABLE registros_bicicleta")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `registros_bicicleta` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`shiftId` INTEGER NOT NULL, `rut` TEXT NOT NULL, `nombre` TEXT NOT NULL, `carrera` TEXT NOT NULL, " +
                "`tipoPersona` TEXT NOT NULL, `observacion` TEXT, `entradaAt` INTEGER NOT NULL, `salidaAt` INTEGER, " +
                "FOREIGN KEY(`shiftId`) REFERENCES `turnos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_bicicleta_shiftId` ON `registros_bicicleta` (`shiftId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_bicicleta_rut` ON `registros_bicicleta` (`rut`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_bicicleta_entradaAt` ON `registros_bicicleta` (`entradaAt`)")
        rows.forEach { r ->
            db.execSQL(
                "INSERT INTO registros_bicicleta (id, shiftId, rut, nombre, carrera, tipoPersona, observacion, entradaAt, salidaAt) " +
                    "VALUES (?,?,?,?,?,?,?,?,?)",
                arrayOf(
                    r.id, r.shiftId, Rut.normalize(r.rut), r.nombre, r.carrera,
                    PersonaType.fromCode(r.tipoPersona).name, r.observacion, r.entradaAt, r.salidaAt
                )
            )
        }
    }

    // endregion

    // region Directorio y examen

    private fun migratePersonas(db: SupportSQLiteDatabase) {
        val updates = db.query("SELECT id, rut, tipoPersona FROM personas").use { c ->
            buildList {
                while (c.moveToNext()) {
                    val tipo = c.strOrNull("tipoPersona")?.takeIf { it.isNotBlank() }?.let { PersonaType.fromCode(it).name }
                    add(arrayOf<Any?>(Rut.normalizeOrNull(c.strOrNull("rut")), tipo, c.long("id")))
                }
            }
        }
        updates.forEach { db.execSQL("UPDATE personas SET rut = ?, tipoPersona = ? WHERE id = ?", it) }
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_personas_rut` ON `personas` (`rut`)")
    }

    private fun migrateExamen(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM examen_preguntas WHERE id NOT IN (SELECT MIN(id) FROM examen_preguntas GROUP BY pregunta)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_examen_preguntas_pregunta` ON `examen_preguntas` (`pregunta`)")

        val resultados = db.query("SELECT * FROM examen_resultados").use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        arrayOf<Any?>(
                            c.long("id"), c.str("guardiaNombre"), parseDateTime(c.str("fecha")) ?: 0L,
                            c.long("puntaje"), c.long("totalPreguntas"),
                            c.getDouble(c.getColumnIndexOrThrow("porcentaje")), c.long("aprobado"),
                            c.long("tiempoEmpleadoSegundos")
                        )
                    )
                }
            }
        }
        db.execSQL("DROP TABLE examen_resultados")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `examen_resultados` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`guardiaNombre` TEXT NOT NULL, `realizadoAt` INTEGER NOT NULL, `puntaje` INTEGER NOT NULL, " +
                "`totalPreguntas` INTEGER NOT NULL, `porcentaje` REAL NOT NULL, `aprobado` INTEGER NOT NULL, " +
                "`tiempoEmpleadoSegundos` INTEGER NOT NULL)"
        )
        resultados.forEach {
            db.execSQL(
                "INSERT INTO examen_resultados (id, guardiaNombre, realizadoAt, puntaje, totalPreguntas, porcentaje, " +
                    "aprobado, tiempoEmpleadoSegundos) VALUES (?,?,?,?,?,?,?,?)",
                it
            )
        }
    }

    // endregion

    // region Conversión de fechas

    private fun toEpochPair(date: LocalDate?, entrada: String, salida: String?, shiftId: Long): Pair<Long, Long?> {
        val entradaTime = parseTime(entrada)
        val entradaAt = if (date != null && entradaTime != null) {
            LocalDateTime.of(date, entradaTime).atZone(zone).toInstant().toEpochMilli()
        } else {
            shiftId.coerceAtLeast(0L)
        }
        val salidaTime = salida?.let(::parseTime)
        val salidaAt = when {
            salida == null -> null
            date == null || entradaTime == null || salidaTime == null -> entradaAt
            else -> {
                val sameDay = LocalDateTime.of(date, salidaTime)
                val exit = if (salidaTime.isBefore(entradaTime)) sameDay.plusDays(1) else sameDay
                exit.atZone(zone).toInstant().toEpochMilli()
            }
        }
        return entradaAt to salidaAt
    }

    private fun parseDate(value: String): LocalDate? {
        val v = value.trim()
        return DATE_PATTERNS.firstNotNullOfOrNull { f -> runCatching { LocalDate.parse(v, f) }.getOrNull() }
    }

    private fun parseTime(value: String): LocalTime? =
        runCatching { LocalTime.parse(value.trim(), TIME) }.getOrNull()

    private fun parseDateTime(value: String): Long? =
        runCatching { LocalDateTime.parse(value.trim(), DATE_TIME).atZone(zone).toInstant().toEpochMilli() }.getOrNull()

    private fun isoDate(epochMs: Long): String =
        java.time.Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().toString()

    // endregion

    private fun Cursor.str(column: String): String = strOrNull(column).orEmpty()
    private fun Cursor.strOrNull(column: String): String? =
        getColumnIndexOrThrow(column).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))

    private companion object {
        const val UNKNOWN_GUARD = "No registrado"
        const val DEFAULT_PORTERIA = "porteria_2"
        const val DEFAULT_BICICLETERO = "bicicletero_1"
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
        val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val DATE_PATTERNS = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
        )
    }
}
