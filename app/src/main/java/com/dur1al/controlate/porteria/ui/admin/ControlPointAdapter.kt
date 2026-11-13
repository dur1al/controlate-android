package com.dur1al.controlate.porteria.ui.admin

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ItemControlPointBinding
import com.dur1al.controlate.porteria.domain.model.ControlPoint
import com.dur1al.controlate.porteria.ui.common.color
import com.dur1al.controlate.porteria.ui.common.labelRes

/**
 * Fila de punto de control: tocar para renombrar; el interruptor activa o desactiva.
 *
 * El interruptor no cambia su propio estado: se revierte al tocarlo y refleja lo que diga la base
 * (fuente única de verdad). Así, si la regla "último activo" rechaza el cambio, no queda mintiendo.
 */
class ControlPointAdapter(
    private val onRename: (ControlPoint) -> Unit,
    private val onToggle: (ControlPoint, Boolean) -> Unit
) : ListAdapter<ControlPoint, ControlPointAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemControlPointBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemControlPointBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(point: ControlPoint) = with(binding) {
            val context = root.context
            val kind = context.getString(point.kind.labelRes)
            tvPointName.text = point.name
            tvPointKind.text = if (point.active) kind else context.getString(R.string.control_points_kind_inactive_format, kind)
            viewColor.backgroundTintList = ColorStateList.valueOf(point.color(context))
            viewColor.alpha = if (point.active) 1f else INACTIVE_ALPHA
            cardPoint.contentDescription = context.getString(R.string.control_points_item_description, point.name, tvPointKind.text)
            cardPoint.setOnClickListener { onRename(point) }

            switchActive.setOnCheckedChangeListener(null)
            switchActive.isChecked = point.active
            switchActive.setOnClickListener {
                val requested = switchActive.isChecked
                switchActive.isChecked = point.active
                onToggle(point, requested)
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ControlPoint>() {
        override fun areItemsTheSame(old: ControlPoint, new: ControlPoint) = old.id == new.id
        override fun areContentsTheSame(old: ControlPoint, new: ControlPoint) = old == new
    }

    private companion object {
        const val INACTIVE_ALPHA = 0.4f
    }
}
