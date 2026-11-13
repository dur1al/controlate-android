package com.dur1al.controlate.porteria.data.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.StringRes
import com.dur1al.controlate.R
import com.dur1al.controlate.porteria.domain.report.ReportDocument
import com.dur1al.controlate.porteria.domain.report.ReportDocument.Column
import com.dur1al.controlate.porteria.domain.report.ReportDocument.MetaField
import com.dur1al.controlate.porteria.domain.report.ReportExporter
import com.dur1al.controlate.porteria.domain.time.Clock
import com.dur1al.controlate.porteria.domain.time.TimeFormat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Dibuja un [ReportDocument] como PDF A4 con `android.graphics.pdf.PdfDocument` (sin dependencias)
 * y lo guarda en el almacenamiento privado de la app (`files/reports`).
 *
 * Todo el trabajo corre en [ioDispatcher]: generar y escribir un PDF de varias páginas en el
 * hilo principal congelaba la UI.
 */
class PdfReportWriter(
    private val context: Context,
    private val clock: Clock,
    private val store: ReportStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ReportExporter {

    override suspend fun export(document: ReportDocument): File = withContext(ioDispatcher) {
        val pdf = PdfDocument()
        try {
            render(pdf, document)
            val file = store.newFile("${document.fileStem}.pdf")
            file.outputStream().use(pdf::writeTo)
            file
        } finally {
            pdf.close()
        }
    }

    private fun render(pdf: PdfDocument, doc: ReportDocument) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val tableWidth = PAGE_WIDTH - 2 * MARGIN
        val totalWeight = doc.columns.sumOf { it.weight.toDouble() }.toFloat()
        val widths = doc.columns.map { tableWidth * it.weight / totalWeight }
        val accent = if (doc.type == ReportDocument.Type.BIKE_SHIFT) BIKE_COLOR else PRIMARY_COLOR
        val rowsPerPage = rowsPerPage(doc)
        val pages = doc.rows.chunked(rowsPerPage).ifEmpty { listOf(emptyList()) }
        val generatedAt = context.getString(R.string.pdf_generated_at, TimeFormat.displayDateTime(clock.now()))

        pages.forEachIndexed { pageIndex, rows ->
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            drawWatermark(canvas)
            var y = drawHeader(canvas, doc)
            y = drawRow(canvas, y, doc.columns.map { context.getString(it.labelRes()) }, widths, headerPaint, accent)
            rows.forEachIndexed { i, row ->
                val isTotal = row.firstOrNull() == ReportDocument.TOTAL_ROW_LABEL
                val cells = if (isTotal) listOf(context.getString(R.string.pdf_total_row)) + row.drop(1) else row
                val background = if ((pageIndex * rowsPerPage + i) % 2 == 1) ZEBRA_COLOR else null
                y = drawRow(canvas, y, cells, widths, if (isTotal) boldPaint else textPaint, background)
            }
            drawFooter(canvas, pageIndex + 1, pages.size, generatedAt)
            pdf.finishPage(page)
        }
    }

    private fun rowsPerPage(doc: ReportDocument): Int {
        val headerHeight = TITLE_HEIGHT + doc.meta.size * META_HEIGHT + SECTION_GAP + ROW_HEIGHT
        return ((PAGE_HEIGHT - 2 * MARGIN - headerHeight - FOOTER_HEIGHT) / ROW_HEIGHT).toInt().coerceAtLeast(1)
    }

    private fun drawHeader(canvas: Canvas, doc: ReportDocument): Float {
        var y = MARGIN + TITLE_HEIGHT - 6f
        canvas.drawText(context.getString(doc.type.titleRes()), MARGIN, y, titlePaint)
        doc.meta.forEach { meta ->
            y += META_HEIGHT
            canvas.drawText(context.getString(meta.field.labelRes(), meta.value), MARGIN, y, textPaint)
        }
        return y + SECTION_GAP
    }

    private fun drawRow(
        canvas: Canvas,
        top: Float,
        cells: List<String>,
        widths: List<Float>,
        paint: TextPaint,
        background: Int?
    ): Float {
        background?.let {
            canvas.drawRect(MARGIN, top, PAGE_WIDTH - MARGIN, top + ROW_HEIGHT, Paint().apply { color = it })
        }
        var x = MARGIN
        val baseline = top + ROW_HEIGHT - 6f
        cells.forEachIndexed { i, text ->
            val width = widths.getOrElse(i) { 0f }
            val fitted = TextUtils.ellipsize(text, paint, width - 2 * CELL_PADDING, TextUtils.TruncateAt.END)
            canvas.drawText(fitted, 0, fitted.length, x + CELL_PADDING, baseline, paint)
            x += width
        }
        return top + ROW_HEIGHT
    }

    private fun drawFooter(canvas: Canvas, page: Int, total: Int, generatedAt: String) {
        val y = PAGE_HEIGHT - MARGIN
        canvas.drawText(context.getString(R.string.pdf_page_of, page, total), MARGIN, y, smallPaint)
        val width = smallPaint.measureText(generatedAt)
        canvas.drawText(generatedAt, PAGE_WIDTH - MARGIN - width, y, smallPaint)
    }

    private fun drawWatermark(canvas: Canvas) {
        val text = context.getString(R.string.pdf_watermark)
        canvas.save()
        canvas.rotate(-45f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)
        canvas.drawText(text, PAGE_WIDTH / 2f - watermarkPaint.measureText(text) / 2f, PAGE_HEIGHT / 2f, watermarkPaint)
        canvas.restore()
    }

    // region Recursos

    @StringRes
    private fun ReportDocument.Type.titleRes() = when (this) {
        ReportDocument.Type.VEHICLE_SHIFT -> R.string.pdf_title_vehicle_shift
        ReportDocument.Type.BIKE_SHIFT -> R.string.pdf_title_bike_shift
        ReportDocument.Type.DAILY_SUMMARY -> R.string.pdf_title_daily_summary
        ReportDocument.Type.DIRECTORY -> R.string.pdf_title_directory
    }

    @StringRes
    private fun MetaField.labelRes() = when (this) {
        MetaField.POINT -> R.string.pdf_meta_point
        MetaField.DATE -> R.string.pdf_meta_date
        MetaField.GUARD -> R.string.pdf_meta_guard
        MetaField.SHIFT_TYPE -> R.string.pdf_meta_shift
    }

    @StringRes
    private fun Column.labelRes() = when (this) {
        Column.INDEX -> R.string.pdf_col_index
        Column.PATENTE -> R.string.pdf_col_patente
        Column.NOMBRE -> R.string.pdf_col_nombre
        Column.RUT -> R.string.pdf_col_rut
        Column.DEPTO -> R.string.pdf_col_depto
        Column.TIPO -> R.string.pdf_col_tipo
        Column.CARRERA -> R.string.pdf_col_carrera
        Column.ENTRADA -> R.string.pdf_col_entrada
        Column.SALIDA -> R.string.pdf_col_salida
        Column.PUNTO -> R.string.pdf_col_punto
        Column.ENTRARON -> R.string.pdf_col_entraron
        Column.SALIERON -> R.string.pdf_col_salieron
        Column.DENTRO -> R.string.pdf_col_dentro
    }

    private val titlePaint = textPaint(16f, Typeface.BOLD, Color.BLACK)
    private val headerPaint = textPaint(9f, Typeface.BOLD, Color.WHITE)
    private val textPaint = textPaint(9f, Typeface.NORMAL, Color.BLACK)
    private val boldPaint = textPaint(9f, Typeface.BOLD, Color.BLACK)
    private val smallPaint = textPaint(8f, Typeface.ITALIC, Color.DKGRAY)
    private val watermarkPaint = textPaint(48f, Typeface.BOLD, Color.argb(28, 0, 0, 0))

    private fun textPaint(size: Float, style: Int, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        typeface = Typeface.create(Typeface.SANS_SERIF, style)
        this.color = color
    }

    // endregion

    private companion object {
        const val PAGE_WIDTH = 595 // A4 a 72 ppp
        const val PAGE_HEIGHT = 842
        const val MARGIN = 36f
        const val TITLE_HEIGHT = 24f
        const val META_HEIGHT = 14f
        const val SECTION_GAP = 12f
        const val ROW_HEIGHT = 18f
        const val FOOTER_HEIGHT = 24f
        const val CELL_PADDING = 4f
        val PRIMARY_COLOR = Color.parseColor("#4A148C")
        val BIKE_COLOR = Color.parseColor("#1B5E20")
        val ZEBRA_COLOR = Color.parseColor("#F2F2F2")
    }
}
