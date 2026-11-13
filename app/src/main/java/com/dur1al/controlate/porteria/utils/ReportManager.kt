package com.dur1al.controlate.porteria.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.core.content.FileProvider
import com.dur1al.controlate.porteria.data.local.entities.Persona
import com.dur1al.controlate.porteria.data.local.entities.RegistroBicicleta
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles PDF report generation natively without external heavy dependencies.
 * Implements clean architecture principles and clean UI for the document.
 */
class ReportManager(private val context: Context) {

    data class SummaryPointRow(val id: String, val label: String, val entradas: Int, val salidas: Int) {
        val dentro: Int get() = (entradas - salidas).coerceAtLeast(0)
    }

    private val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595x842 at 72 PPI
    private val margin = 40f
    private val primaryColor = Color.parseColor("#4A148C") // Dark violet for headers
    private val secondaryColor = Color.parseColor("#1B5E20") // Dark green for bikes

    private val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 16f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }

    private val headerPaint = Paint().apply {
        color = Color.WHITE
        textSize = 10f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 9f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        isAntiAlias = true
    }

    private val watermarkPaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        textSize = 40f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        alpha = 50 // Semi-transparent
        isAntiAlias = true
    }

    private val watermarkTextPaint = Paint().apply {
        color = Color.GRAY
        textSize = 8f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT
    }

    /**
     * Generates a PDF report for regular entry points.
     */
    fun generateAndShareExcel( // Kept the method name identical to avoid breaking other files, though it now exports PDF
        registros: List<RegistroIngreso>,
        guardiaName: String,
        porteriaName: String,
        fechaTurno: String,
        turnoTipo: String
    ): ExportResult {
        if (registros.isEmpty()) return ExportResult.EmptyData
        return try {
            val document = PdfDocument()
            val headerInfo = listOf(
                "CONTROLATE APP - REPORTE DE INGRESO",
                "Punto: $porteriaName",
                "Fecha: $fechaTurno",
                "Guardia Responsable: $guardiaName",
                "Turno: $turnoTipo"
            )
            
            val headers = arrayOf("N", "HORA", "PATENTE", "NOMBRE", "DEPTO", "RUT", "TIPO", "GUARDIA")
            val columnWidths = floatArrayOf(20f, 60f, 50f, 100f, 50f, 60f, 60f, 60f)
            
            generatePdfPages(document, registros, headerInfo, headers, columnWidths, primaryColor) { reg, index ->
                arrayOf(
                    (index + 1).toString(),
                    "${reg.horaEntrada}/${reg.horaSalida ?: "--"}",
                    reg.patente,
                    reg.nombreApellido,
                    reg.departamento,
                    reg.rut ?: "-",
                    reg.tipoPersona ?: "Visita",
                    reg.guardiaTurno
                )
            }

            val fileName = "Reporte_${sanitizeFileName(porteriaName)}_${sanitizeFileName(fechaTurno)}.pdf"
            val file = saveDocument(document, fileName, true)
            ExportResult.Success(file)
        } catch (e: Exception) {
            Log.e("ReportManager", "Error generating PDF: ${e.message}")
            ExportResult.Error(e.message ?: "Error al generar reporte")
        }
    }

    /**
     * Generates a PDF report for bike entry points.
     */
    fun generateBicicletaReportExcel(
        registros: List<RegistroBicicleta>,
        guardiaName: String = "No registrado",
        turnoTipo: String = "No definido",
        shareAfterSave: Boolean = false
    ): ExportResult {
        if (registros.isEmpty()) return ExportResult.EmptyData
        return try {
            val document = PdfDocument()
            val first = registros.first()
            val displayName = PointDisplayHelper.getDisplayName(context, first.controlPoint)

            val headerInfo = listOf(
                "CONTROLATE APP - REPORTE BICICLETERO",
                "Punto: $displayName",
                "Fecha: ${first.fecha}",
                "Guardia Responsable: $guardiaName",
                "Turno: $turnoTipo"
            )

            val headers = arrayOf("N", "RUT", "NOMBRE", "TIPO", "CARRERA", "ENTRO", "SALIO")
            val columnWidths = floatArrayOf(30f, 70f, 140f, 60f, 100f, 40f, 40f)

            generatePdfPages(document, registros, headerInfo, headers, columnWidths, secondaryColor) { reg, index ->
                arrayOf(
                    (index + 1).toString(),
                    reg.rut,
                    reg.nombre,
                    reg.tipoPersona,
                    reg.carrera,
                    reg.horaEntrada,
                    reg.horaSalida ?: "--:--"
                )
            }

            val stamp = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
            val file = saveDocument(document, "Reporte_Bicicletas_$stamp.pdf", shareAfterSave)
            ExportResult.Success(file)
        } catch (e: Exception) {
            Log.e("ReportManager", "Error generating bike PDF: ${e.message}")
            ExportResult.Error(e.message ?: "Error al generar reporte")
        }
    }

    /**
     * Generates a comprehensive summary PDF for admins.
     */
    fun generateAdminSummaryExcel(
        pointStats: List<SummaryPointRow>,
        registrosPorteria: List<RegistroIngreso>,
        registrosBici: List<RegistroBicicleta>,
        fechaPorteria: String,
        fechaBici: String
    ): ExportResult {
        return try {
            val document = PdfDocument()
            
            // Page 1: Summary
            val summaryHeader = listOf(
                "CONTROLATE APP - RESUMEN ADMINISTRATIVO",
                "Fecha Ingresos: $fechaPorteria",
                "Fecha Bicicletas: $fechaBici"
            )
            val sHeaders = arrayOf("PUNTO", "ENTRARON", "SALIERON", "DENTRO")
            val sWidths = floatArrayOf(200f, 80f, 80f, 80f)
            
            generatePdfPages(document, pointStats, summaryHeader, sHeaders, sWidths, primaryColor) { stat, _ ->
                arrayOf(stat.label, stat.entradas.toString(), stat.salidas.toString(), stat.dentro.toString())
            }

            // Save document
            val stamp = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
            val file = saveDocument(document, "Resumen_Admin_$stamp.pdf", true)
            ExportResult.Success(file)
        } catch (e: Exception) {
            Log.e("ReportManager", "Error generating admin summary: ${e.message}")
            ExportResult.Error(e.message ?: "Error al generar reporte")
        }
    }

    /**
     * Exports the directory of people to PDF.
     */
    fun exportDatabaseExcel(personas: List<Persona>): ExportResult {
        if (personas.isEmpty()) return ExportResult.EmptyData
        return try {
            val document = PdfDocument()
            val headerInfo = listOf("CONTROLATE APP - DIRECTORIO (BASE DE DATOS)")
            val headers = arrayOf("PATENTE", "NOMBRE", "DEPTO", "RUT", "TIPO")
            val columnWidths = floatArrayOf(60f, 160f, 80f, 80f, 80f)

            generatePdfPages(document, personas, headerInfo, headers, columnWidths, primaryColor) { p, _ ->
                arrayOf(p.patente, p.nombreApellido, p.departamento, p.rut ?: "", p.tipoPersona ?: "")
            }

            val stamp = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
            val file = saveDocument(document, "Directory_$stamp.pdf", true)
            ExportResult.Success(file)
        } catch (e: Exception) {
            Log.e("ReportManager", "Error exporting directory: ${e.message}")
            ExportResult.Error(e.message ?: "Error generating report")
        }
    }

    /**
     * Core PDF generation loop handling pagination and drawing tables.
     */
    private fun <T> generatePdfPages(
        document: PdfDocument,
        items: List<T>,
        headerInfo: List<String>,
        headers: Array<String>,
        columnWidths: FloatArray,
        headerColor: Int,
        rowMapper: (T, Int) -> Array<String>
    ) {
        val rowHeight = 20f
        val maxRowsPerPage = 32
        val totalPages = Math.ceil(items.size.toDouble() / maxRowsPerPage).toInt().coerceAtLeast(1)

        for (pageIndex in 0 until totalPages) {
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawWatermark(canvas)

            var currentY = margin

            // Draw Header Info
            headerInfo.forEachIndexed { i, info ->
                if (i == 0) {
                    canvas.drawText(info, margin, currentY, titlePaint)
                } else {
                    canvas.drawText(info, margin, currentY, textPaint)
                }
                currentY += 15f
            }
            currentY += 10f

            // Draw Table Header
            val headerBgPaint = Paint().apply { color = headerColor }
            canvas.drawRect(margin, currentY - 12f, pageInfo.pageWidth - margin, currentY + 8f, headerBgPaint)
            
            var currentX = margin
            headers.forEachIndexed { i, header ->
                canvas.drawText(header, currentX + 5f, currentY, headerPaint)
                currentX += columnWidths[i]
            }
            currentY += rowHeight

            // Draw Data Rows
            val startItemIndex = pageIndex * maxRowsPerPage
            val endItemIndex = minOf(startItemIndex + maxRowsPerPage, items.size)
            
            for (i in startItemIndex until endItemIndex) {
                val rowData = rowMapper(items[i], i)
                currentX = margin

                // Draw row background for alternate rows
                if (i % 2 == 1) {
                    val altBgPaint = Paint().apply { color = Color.parseColor("#F5F5F5") }
                    canvas.drawRect(margin, currentY - 12f, pageInfo.pageWidth - margin, currentY + 8f, altBgPaint)
                }

                rowData.forEachIndexed { colIndex, text ->
                    // Truncate text if it's too long for the column
                    val truncatedText = text.take(25) + if(text.length > 25) "..." else ""
                    canvas.drawText(truncatedText, currentX + 5f, currentY, textPaint)
                    currentX += columnWidths[colIndex]
                }
                currentY += rowHeight
            }

            // Draw footer with page number and signature
            val footerY = pageInfo.pageHeight - margin
            canvas.drawText("Página ${pageIndex + 1} de $totalPages", margin, footerY, textPaint)
            canvas.drawText("Generado por dur1al", pageInfo.pageWidth - margin, footerY, watermarkTextPaint)

            document.finishPage(page)
        }
    }

    private fun drawWatermark(canvas: Canvas) {
        canvas.save()
        canvas.translate(pageInfo.pageWidth / 2f, pageInfo.pageHeight / 2f)
        canvas.rotate(-45f)
        
        val text = "CONTROLATE APP"
        val textWidth = watermarkPaint.measureText(text)
        canvas.drawText(text, -textWidth / 2f, 0f, watermarkPaint)
        
        canvas.restore()
    }

    fun listSavedReports(): List<File> {
        val dir = getReportsDir()
        val files = dir.listFiles()?.filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }.orEmpty()
        return files.sortedByDescending { it.lastModified() }
    }

    fun shareExistingReport(file: File) {
        if (!file.exists()) return
        shareFile(file)
    }

    private fun saveDocument(document: PdfDocument, fileName: String, shareAfterSave: Boolean): File {
        val file = File(getReportsDir(), fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        if (shareAfterSave) shareFile(file)
        return file
    }

    private fun shareFile(file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF Report"))
        }.onFailure { error ->
            Log.e("ReportManager", "Could not share report ${file.name}: ${error.message}")
        }
    }

    private fun getReportsDir(): File {
        val dir = File(context.filesDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun sanitizeFileName(value: String): String {
        return value.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
