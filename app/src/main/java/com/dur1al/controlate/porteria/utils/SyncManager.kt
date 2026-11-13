package com.dur1al.controlate.porteria.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class SyncManager(private val context: Context) {
    private val gson = Gson()

    fun exportDatabaseToJson(registros: List<RegistroIngreso>) {
        val fileName = "Respaldo_Porteria_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.json"
        val file = File(context.cacheDir, fileName)

        try {
            val writer = FileWriter(file)
            gson.toJson(registros, writer)
            writer.flush()
            writer.close()

            shareJsonFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareJsonFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Exportar Base de Datos (JSON)"))
    }

    fun importJsonToRecords(file: File): List<RegistroIngreso>? {
        return try {
            val reader = FileReader(file)
            val type = object : TypeToken<List<RegistroIngreso>>() {}.type
            val registros: List<RegistroIngreso> = gson.fromJson(reader, type)
            reader.close()
            registros
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
