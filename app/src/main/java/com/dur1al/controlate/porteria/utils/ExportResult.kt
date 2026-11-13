package com.dur1al.controlate.porteria.utils

import java.io.File

sealed class ExportResult {
    data class Success(val file: File) : ExportResult()
    data class Error(val message: String) : ExportResult()
    object EmptyData : ExportResult()
}
