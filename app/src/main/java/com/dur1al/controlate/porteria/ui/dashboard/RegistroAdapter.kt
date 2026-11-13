package com.dur1al.controlate.porteria.ui.dashboard

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ItemRegistroBinding
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso

class RegistroAdapter(
    private val onExitClick: (RegistroIngreso) -> Unit,
    private val onItemClick: (RegistroIngreso) -> Unit,
    private val onLongClick: (RegistroIngreso) -> Unit
) : ListAdapter<RegistroIngreso, RegistroAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRegistroBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemRegistroBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RegistroIngreso) {
            binding.apply {
                tvPatente.text = item.patente.uppercase()
                tvNombre.text = item.nombreApellido
                
                val tipo = item.tipoPersona ?: "Visita"
                tvTipoPersona.text = tipo
                
                val context = root.context
                val (bgRes, colorRes) = when (tipo.lowercase()) {
                    "visita" -> Pair(R.drawable.bg_tag_visit, R.color.warning_orange)
                    "estudiante" -> Pair(R.drawable.bg_tag_student, R.color.success_green)
                    "funcionario" -> Pair(R.drawable.bg_tag_employee, R.color.primary_light)
                    else -> Pair(R.drawable.bg_tag_generic, R.color.text_main)
                }
                tvTipoPersona.setBackgroundResource(bgRes)
                tvTipoPersona.setTextColor(ContextCompat.getColor(context, colorRes))
                
                val green = Color.parseColor("#81C784") // Verde suave
                val red = Color.parseColor("#E57373")   // Rojo suave
                
                val text = "${item.departamento} | E: ${item.horaEntrada}" + 
                          (if (item.horaSalida != null) " - S: ${item.horaSalida}" else "")
                
                val spannable = SpannableString(text)
                
                // Colorear E: Hora Entrada
                val eIndex = text.indexOf("E: ")
                if (eIndex != -1) {
                    spannable.setSpan(ForegroundColorSpan(green), eIndex + 3, eIndex + 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                
                // Colorear S: Hora Salida
                if (item.horaSalida != null) {
                    val sIndex = text.indexOf("S: ")
                    if (sIndex != -1) {
                        spannable.setSpan(ForegroundColorSpan(red), sIndex + 3, sIndex + 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    btnExit.isEnabled = false
                    btnExit.alpha = 0.3f
                    
                    tvStatus.setText(R.string.status_exited)
                    tvStatus.setBackgroundResource(R.drawable.bg_status_exited)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_exited_text))
                } else {
                    btnExit.isEnabled = true
                    btnExit.alpha = 1.0f
                    
                    tvStatus.setText(R.string.status_inside)
                    tvStatus.setBackgroundResource(R.drawable.bg_status_inside)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_inside_text))
                }

                tvDetalle.text = spannable

                btnExit.setOnClickListener { onExitClick(item) }
                root.setOnClickListener { onItemClick(item) }
                root.setOnLongClickListener {
                    onLongClick(item)
                    true
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RegistroIngreso>() {
        override fun areItemsTheSame(oldItem: RegistroIngreso, newItem: RegistroIngreso): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RegistroIngreso, newItem: RegistroIngreso): Boolean {
            return oldItem == newItem
        }
    }
}
