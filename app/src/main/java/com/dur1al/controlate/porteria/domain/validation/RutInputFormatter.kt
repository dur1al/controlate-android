package com.dur1al.controlate.porteria.domain.validation

/**
 * Formateo del RUT mientras se escribe (`12.345.678-5`), como función pura para poder probar
 * cada tecla sin Android. Reglas:
 *
 * - Solo se conservan dígitos y una `K` como último carácter (una `K` intermedia se descarta).
 * - Se limita a [Rut.MAX_LENGTH] caracteres útiles; lo que sobra se ignora.
 * - El cursor queda después del mismo carácter útil que tenía a su izquierda, así que escribir,
 *   borrar o pegar en cualquier posición no desordena los dígitos.
 * - Si se borra un separador (`.` o `-`), se borra el dígito anterior: de lo contrario el texto
 *   se volvería a formatear igual y el borrado no tendría efecto.
 */
object RutInputFormatter {

    data class Result(val text: String, val cursor: Int)

    /**
     * @param text texto del campo tras la edición.
     * @param cursor posición del cursor tras la edición.
     * @param deletedSeparator `true` si la edición fue borrar exactamente un `.` o `-`.
     */
    fun format(text: String, cursor: Int, deletedSeparator: Boolean = false): Result {
        var input = text
        var caret = cursor.coerceIn(0, text.length)
        if (deletedSeparator) {
            val previous = (caret - 1 downTo 0).firstOrNull { isSignificant(input[it]) }
            if (previous != null) {
                input = input.removeRange(previous, previous + 1)
                caret = previous
            }
        }

        val significantBeforeCaret = input.take(caret).count(::isSignificant)
        val chars = input.uppercase().filter(::isSignificant)
        // La K solo vale como dígito verificador (último carácter).
        val clean = chars.filterIndexed { i, c -> c != 'K' || i == chars.lastIndex }.take(Rut.MAX_LENGTH)
        val droppedBeforeCaret = chars.take(significantBeforeCaret)
            .filterIndexed { i, c -> c == 'K' && i != chars.lastIndex }.length
        val keptBeforeCaret = (significantBeforeCaret - droppedBeforeCaret).coerceIn(0, clean.length)

        val formatted = Rut.format(clean)
        return Result(formatted, positionAfter(formatted, keptBeforeCaret))
    }

    /** `true` para los caracteres que forman parte del RUT (dígitos y `K`). */
    fun isSignificant(c: Char): Boolean = c.isDigit() || c == 'K' || c == 'k'

    /** Índice en [formatted] justo después del carácter útil número [count]. */
    private fun positionAfter(formatted: String, count: Int): Int {
        if (count <= 0) return 0
        var seen = 0
        formatted.forEachIndexed { index, c ->
            if (isSignificant(c) && ++seen == count) return index + 1
        }
        return formatted.length
    }
}
