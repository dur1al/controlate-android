package com.dur1al.controlate.porteria.utils

object RutUtils {
    /**
     * Formatea un RUT a formato XX.XXX.XXX-X
     */
    fun formatRut(rut: String): String {
        val clean = rut.replace(Regex("[^0-9kK]"), "").uppercase()
        if (clean.length < 2) return clean
        
        val cuerpo = clean.substring(0, clean.length - 1)
        val dv = clean.substring(clean.length - 1)
        
        var formateado = ""
        var contador = 0
        for (i in cuerpo.length - 1 downTo 0) {
            formateado = cuerpo[i] + formateado
            contador++
            if (contador == 3 && i != 0) {
                formateado = ".$formateado"
                contador = 0
            }
        }
        return "$formateado-$dv"
    }

    /**
     * Limpia el RUT para guardar solo números y la letra K
     */
    fun cleanRut(rut: String): String {
        return rut.replace(Regex("[^0-9kK]"), "").uppercase()
    }

    fun isValidRut(rut: String): Boolean {
        if (rut.length < 2) return false
        val clean = rut.replace(Regex("[^0-9kK]"), "")
        val cuerpo = clean.substring(0, clean.length - 1)
        val dv = clean.last().uppercaseChar()
        
        var m = 0
        var s = 1
        var i = cuerpo.length - 1
        while (i >= 0) {
            s = (s + cuerpo[i].toString().toInt() * (9 - m % 6)) % 11
            m++
            i--
        }
        val expectedDv = if (s > 0) (s - 1).toString() else "K"
        return dv.toString() == expectedDv
    }
}
