package com.dur1al.controlate.porteria.ui.porteria

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ItemRegistroBinding
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso
import com.dur1al.controlate.porteria.domain.model.PersonaType
import com.dur1al.controlate.porteria.domain.time.TimeFormat
import com.dur1al.controlate.porteria.ui.common.labelRes

class RegistroAdapter(
    private val onExitClick: (RegistroIngreso) -> Unit,
    private val onItemClick: (RegistroIngreso) -> Unit
) : ListAdapter<RegistroIngreso, RegistroAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemRegistroBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemRegistroBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RegistroIngreso) = with(binding) {
            val context = root.context
            val type = PersonaType.fromCode(item.tipoPersona)
            tvPatente.text = item.patente
            tvNombre.text = item.nombreApellido
            tvTipoPersona.setText(type.labelRes)
            val (bg, color) = when (type) {
                PersonaType.VISITA -> R.drawable.bg_tag_visit to R.color.warning_orange
                PersonaType.ESTUDIANTE -> R.drawable.bg_tag_student to R.color.success_green
                PersonaType.FUNCIONARIO -> R.drawable.bg_tag_employee to R.color.primary_light
                else -> R.drawable.bg_tag_generic to R.color.text_main
            }
            tvTipoPersona.setBackgroundResource(bg)
            tvTipoPersona.setTextColor(ContextCompat.getColor(context, color))

            val entrada = TimeFormat.time(item.entradaAt)
            tvDetalle.text = item.salidaAt?.let {
                context.getString(R.string.registro_detail_exit_format, item.departamento, entrada, TimeFormat.time(it))
            } ?: context.getString(R.string.registro_detail_format, item.departamento, entrada)

            val inside = item.salidaAt == null
            btnExit.isEnabled = inside
            btnExit.alpha = if (inside) 1f else DISABLED_ALPHA
            tvStatus.setText(if (inside) R.string.status_inside else R.string.status_exited)
            tvStatus.setBackgroundResource(if (inside) R.drawable.bg_status_inside else R.drawable.bg_status_exited)
            tvStatus.setTextColor(
                ContextCompat.getColor(context, if (inside) R.color.status_inside_text else R.color.status_exited_text)
            )

            btnExit.setOnClickListener { onExitClick(item) }
            root.setOnClickListener { onItemClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<RegistroIngreso>() {
        override fun areItemsTheSame(old: RegistroIngreso, new: RegistroIngreso) = old.id == new.id
        override fun areContentsTheSame(old: RegistroIngreso, new: RegistroIngreso) = old == new
    }

    private companion object {
        const val DISABLED_ALPHA = 0.3f
    }
}
