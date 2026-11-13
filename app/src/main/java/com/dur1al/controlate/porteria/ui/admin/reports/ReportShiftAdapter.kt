package com.dur1al.controlate.porteria.ui.admin.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ItemShiftBinding

class ReportShiftAdapter(
    private val onExportClick: (ReportShiftUi) -> Unit,
    private val onDeleteClick: (ReportShiftUi) -> Unit
) : ListAdapter<ReportShiftUi, ReportShiftAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShiftBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onExportClick, onDeleteClick)
    }

    class ViewHolder(private val binding: ItemShiftBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: ReportShiftUi,
            onExportClick: (ReportShiftUi) -> Unit,
            onDeleteClick: (ReportShiftUi) -> Unit
        ) {
            val context = binding.root.context
            val isBike = item.type == ReportType.BICICLETERO
            val badgeColor = if (isBike) R.color.bicicletero2_primary else R.color.primary
            val closedLabel = "${item.totalClosed}/${item.totalRecords} salidas"
            val typeLabel = if (isBike) "BICICLETERO" else "PORTERIA"
            val horaFin = item.endTime ?: "--:--"

            binding.tvReportType.text = typeLabel
            binding.tvReportType.setBackgroundColor(ContextCompat.getColor(context, badgeColor))
            binding.tvRecordCount.text = closedLabel
            binding.tvGuardName.text = context.getString(R.string.report_guard_label, item.title)
            binding.tvShiftInfo.text = context.getString(R.string.report_shift_info, item.date, item.point)
            binding.tvShiftTime.text = "${item.shiftLabel.uppercase()} | ${item.startTime} a $horaFin"
            binding.btnExportExcel.setOnClickListener { onExportClick(item) }
            binding.root.setOnLongClickListener {
                onDeleteClick(item)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ReportShiftUi>() {
        override fun areItemsTheSame(oldItem: ReportShiftUi, newItem: ReportShiftUi): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReportShiftUi, newItem: ReportShiftUi): Boolean {
            return oldItem == newItem
        }
    }
}
