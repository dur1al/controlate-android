package com.dur1al.controlate.porteria.ui.admin.reports

enum class ReportType { PORTERIA, BICICLETERO }

data class ReportShiftUi(
    val id: String,
    val shiftId: Long,
    val type: ReportType,
    val title: String,
    val point: String,
    val date: String,
    val shiftLabel: String,
    val startTime: String,
    val endTime: String?,
    val totalRecords: Int,
    val totalClosed: Int
)
