package com.dur1al.controlate.porteria.data.report

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Carpeta privada de reportes (`files/reports`). Se comparte hacia otras apps solo vía
 * `FileProvider` (ver `res/xml/file_paths.xml`), nunca con rutas de archivo directas.
 */
class ReportStore(
    private val directory: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun newFile(name: String): File {
        directory.mkdirs()
        return File(directory, name)
    }

    /** PDFs guardados, del más reciente al más antiguo. */
    suspend fun list(): List<File> = withContext(ioDispatcher) {
        directory.listFiles { f -> f.isFile && f.extension.equals("pdf", ignoreCase = true) }
            .orEmpty()
            .sortedByDescending { it.lastModified() }
    }
}
