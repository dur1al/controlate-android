package com.dur1al.controlate.porteria.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dur1al.controlate.R
import com.dur1al.controlate.databinding.ItemAdminLogBinding
import com.dur1al.controlate.porteria.data.local.entities.RegistroIngreso

/**
 * Adapter para mostrar los registros recientes en el Dashboard de Administrador.
 * Enfocado en dar visibilidad clara de patentes y responsables (supervisión).
 */
class AdminLogAdapter : ListAdapter<RegistroIngreso, AdminLogAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemAdminLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(registro: RegistroIngreso) {
            binding.tvPlate.text = registro.patente
            binding.tvName.text = registro.nombreApellido
            binding.tvGuard.text = "Registrado por: ${registro.guardiaTurno}"
            
            if (registro.horaSalida != null) {
                binding.tvStatus.text = binding.root.context.getString(R.string.status_exited)
                binding.cvStatus.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.text_secondary))
                binding.tvTime.text = "${registro.horaEntrada} - ${registro.horaSalida}"
            } else {
                binding.tvStatus.text = binding.root.context.getString(R.string.status_inside)
                binding.cvStatus.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.error_red))
                binding.tvTime.text = "Entró: ${registro.horaEntrada}"
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
