package com.dur1al.controlate.porteria.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ItemShiftBinding
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.domain.model.ShiftSummary
import com.dur1al.controlate.porteria.domain.model.ShiftType
import com.dur1al.controlate.porteria.domain.time.TimeFormat
import com.dur1al.controlate.porteria.ui.common.controlPointLabel
import com.dur1al.controlate.porteria.ui.common.labelRes

/** Tarjeta de turno en el listado de reportes. Mantener presionado para eliminar. */
class ShiftSummaryAdapter(
    private val onExport: (ShiftSummary) -> Unit,
    private val onDelete: (ShiftSummary) -> Unit
) : ListAdapter<ShiftSummary, ShiftSummaryAdapter.ViewHolder>(Diff) {

    /** Nombres actuales de los puntos; al cambiar (p. ej. un renombre) se vuelven a dibujar las tarjetas. */
    var points: Map<String, ControlPoint> = emptyMap()
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemShiftBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemShiftBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShiftSummary) = with(binding) {
            val context = root.context
            val kind = points[item.controlPoint.trim().lowercase()]?.kind ?: ControlPoint.Kind.fromId(item.controlPoint)
            val isBike = kind == ControlPoint.Kind.BICICLETERO
            tvReportType.setText(if (isBike) R.string.report_type_bicicletero else R.string.report_type_porteria)
            tvReportType.setBackgroundColor(
                ContextCompat.getColor(context, if (isBike) R.color.bicicletero2_primary else R.color.primary)
            )
            tvRecordCount.text = context.getString(R.string.report_closed_count, item.totalSalidas, item.totalRegistros)
            tvGuardName.text = context.getString(R.string.report_guard_label, item.guardia)
            tvShiftInfo.text = context.getString(
                R.string.report_shift_info, TimeFormat.isoToDisplay(item.fecha), context.controlPointLabel(item.controlPoint, points)
            )
            tvShiftTime.text = context.getString(
                R.string.report_shift_time_format,
                context.getString(ShiftType.fromCode(item.tipo).labelRes),
                TimeFormat.time(item.iniciadoAt),
                item.finalizadoAt?.let { TimeFormat.time(it) } ?: context.getString(R.string.report_shift_open)
            )
            btnExportExcel.setOnClickListener { onExport(item) }
            root.setOnLongClickListener {
                onDelete(item)
                true
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ShiftSummary>() {
        override fun areItemsTheSame(old: ShiftSummary, new: ShiftSummary) = old.shiftId == new.shiftId
        override fun areContentsTheSame(old: ShiftSummary, new: ShiftSummary) = old == new
    }
}
